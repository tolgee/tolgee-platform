/**
 * Copyright (C) 2026 Tolgee s.r.o. and contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.tolgee.security.oauth2

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.MACSigner
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import io.tolgee.development.testDataBuilder.data.BaseTestData
import io.tolgee.dtos.request.translation.comment.TranslationCommentDto
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsNotFound
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.andIsUnauthorized
import io.tolgee.model.UserAccount
import io.tolgee.model.batch.BatchJob
import io.tolgee.model.enums.ProjectPermissionType
import io.tolgee.model.translation.Translation
import io.tolgee.model.translation.TranslationComment
import io.tolgee.testing.AbstractControllerTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm
import org.springframework.security.oauth2.jwt.JwsHeader
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date

/**
 * Verifies the RS256-vs-HS512 discrimination in AuthenticationFilter and the token-scope/project-set intersection in
 * SecurityService. Tokens are minted directly with the server's JwtEncoder so the resolver path is exercised without
 * running the full authorization-code dance.
 */
class OAuth2AccessTokenAuthTest : AbstractControllerTest() {
  @Autowired
  private lateinit var jwtEncoder: JwtEncoder

  @Autowired
  private lateinit var jdbcTemplate: JdbcTemplate

  private lateinit var testData: BaseTestData
  private lateinit var viewOnlyUser: UserAccount
  private lateinit var adminUser: UserAccount
  private lateinit var supporterUser: UserAccount
  private lateinit var langRestrictedAdmin: UserAccount
  private lateinit var ownComment: TranslationComment
  private lateinit var ownCommentTranslation: Translation
  private lateinit var ownBatchJob: BatchJob

  @BeforeEach
  fun setup() {
    testData = BaseTestData()
    // A second user with view-only project access, to prove a token can't grant more than the user actually has.
    viewOnlyUser = testData.root.addUserAccount { username = "oauth_view_only_user" }.self
    // A server admin and supporter who are NOT members of testData.project, to prove an OAuth token can't ride their
    // server-wide reach onto a project they never joined.
    adminUser = testData.root.addUserAccount { username = "oauth_admin_user"; role = UserAccount.Role.ADMIN }.self
    supporterUser =
      testData.root.addUserAccount { username = "oauth_supporter_user"; role = UserAccount.Role.SUPPORTER }.self
    // A server admin who is ALSO a language-restricted member (TRANSLATE, German only), to prove an OAuth token honors
    // the per-language restriction rather than riding the admin bypass.
    val german = testData.projectBuilder.addLanguage { name = "German"; tag = "de"; originalName = "German" }.self
    langRestrictedAdmin =
      testData.root.addUserAccount { username = "oauth_lang_admin"; role = UserAccount.Role.ADMIN }.self
    testData.projectBuilder.addPermission {
      user = langRestrictedAdmin
      type = ProjectPermissionType.TRANSLATE
      translateLanguages = mutableSetOf(german)
    }
    testData.projectBuilder.addPermission {
      user = viewOnlyUser
      type = ProjectPermissionType.VIEW
    }
    testData.projectBuilder
      .addKey { name = "oauth-own-comment-key" }
      .build {
        addTranslation {
          language = testData.projectBuilder.self.baseLanguage!!
          text = "value"
          ownCommentTranslation = this
        }.build {
          ownComment =
            addComment {
              text = "comment by token owner"
              author = testData.user
            }.self
        }
      }
    ownBatchJob =
      testData.projectBuilder
        .addBatchJob {
          author = testData.user
          totalItems = 1
        }.self
    testDataService.saveTestData(testData.root)
    // Tokens are minted directly (bypassing SAS), so no real authorization row exists — insert one the liveness check
    // can find, since the resolver now rejects any OAuth token whose authorization is gone.
    jdbcTemplate.update("DELETE FROM oauth2_authorization WHERE id = ?", LIVE_AUTHORIZATION_ID)
    jdbcTemplate.update(
      "INSERT INTO oauth2_authorization (id, registered_client_id, principal_name, authorization_grant_type) " +
        "VALUES (?, ?, ?, ?)",
      LIVE_AUTHORIZATION_ID,
      "test-client",
      testData.user.id.toString(),
      "authorization_code",
    )
  }

  @AfterEach
  fun cleanup() {
    jdbcTemplate.update("DELETE FROM oauth2_authorization WHERE id = ?", LIVE_AUTHORIZATION_ID)
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `accepts a valid scoped RS256 token`() {
    val token = mint(scopes = listOf("translations.view"), projects = OAuth2Constants.ALL_PROJECTS)
    performGet(translationsUrl(), bearer(token)).andIsOk
  }

  @Test
  fun `rejects a wrong-audience token`() {
    val token =
      mint(
        scopes = listOf("translations.view"),
        projects = OAuth2Constants.ALL_PROJECTS,
        audience = "https://not-tolgee",
      )
    performGet(translationsUrl(), bearer(token)).andIsUnauthorized
  }

  @Test
  fun `rejects an RS256 token signed by a key outside the JWKS`() {
    val keyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
    val claims =
      JWTClaimsSet
        .Builder()
        .subject(testData.user.id.toString())
        .audience(apiAudience)
        .issueTime(Date())
        .expirationTime(Date(System.currentTimeMillis() + 600_000))
        .claim("scope", listOf("translations.view"))
        .claim(OAuth2Constants.PROJECTS_CLAIM, OAuth2Constants.ALL_PROJECTS)
        .build()
    val forged = SignedJWT(JWSHeader.Builder(JWSAlgorithm.RS256).build(), claims)
    forged.sign(RSASSASigner(keyPair.private as RSAPrivateKey))

    performGet(translationsUrl(), bearer(forged.serialize())).andIsUnauthorized
  }

  @Test
  fun `rejects an HS-signed token even when it bears the API audience`() {
    // The aud claim routes this into the OAuth path; the RSA-only JWKS decoder must still reject an HMAC signature
    // (an algorithm-confusion attempt), not accept it.
    val claims =
      JWTClaimsSet
        .Builder()
        .subject(testData.user.id.toString())
        .audience(apiAudience)
        .issueTime(Date())
        .expirationTime(Date(System.currentTimeMillis() + 600_000))
        .claim("scope", listOf("translations.view"))
        .claim(OAuth2Constants.PROJECTS_CLAIM, OAuth2Constants.ALL_PROJECTS)
        .build()
    val forged = SignedJWT(JWSHeader(JWSAlgorithm.HS256), claims)
    forged.sign(MACSigner("an-attacker-chosen-secret-at-least-32-bytes-long!!"))

    performGet(translationsUrl(), bearer(forged.serialize())).andIsUnauthorized
  }

  @Test
  fun `is forbidden on endpoints not opened to any API token`() {
    val token = mint(scopes = listOf("translations.view"), projects = OAuth2Constants.ALL_PROJECTS)
    performGet("/v2/projects", bearer(token)).andIsForbidden
  }

  @Test
  fun `cannot exceed the user's live permissions`() {
    val token =
      mint(scopes = listOf("members.view"), projects = OAuth2Constants.ALL_PROJECTS, subject = viewOnlyUser.id)
    performGet("/v2/projects/${testData.project.id}/users", bearer(token)).andIsForbidden
  }

  @Test
  fun `fails closed on an unrecognized project claim shape`() {
    val token = mint(scopes = listOf("translations.view"), projects = 999L)
    performGet(translationsUrl(), bearer(token)).andIsNotFound
  }

  @Test
  fun `grants access to a project inside the token project set`() {
    val token = mint(scopes = listOf("translations.view"), projects = listOf(testData.project.id))
    performGet(translationsUrl(), bearer(token)).andIsOk
  }

  @Test
  fun `hides a project outside the token project set`() {
    val token = mint(scopes = listOf("translations.view"), projects = listOf(testData.project.id + 999))
    performGet(translationsUrl(), bearer(token)).andIsNotFound
  }

  @Test
  fun `narrows scopes below the user's live permissions`() {
    val token = mint(scopes = listOf("members.view"), projects = OAuth2Constants.ALL_PROJECTS)
    performGet(translationsUrl(), bearer(token)).andIsForbidden
  }

  @Test
  fun `serves current-permissions for an OAuth token`() {
    val token = mint(scopes = listOf("translations.view"), projects = OAuth2Constants.ALL_PROJECTS)
    performGet("/v2/api-keys/current-permissions?projectId=${testData.project.id}", bearer(token))
      .andIsOk
      .andAssertThatJson {
        node("projectId").isNumber
        node("scopes").isArray.contains("translations.view").doesNotContain("keys.edit", "admin")
      }
  }

  @Test
  fun `denies current-permissions for a project outside the token project set`() {
    // The token is narrowed to a different project. Even though the underlying user can access testData.project, the
    // endpoint must not disclose its name, the user's role or permitted languages — it has no project path variable, so
    // the interceptor never narrows it; the controller must reject outright, not just return empty scopes.
    val token = mint(scopes = listOf("translations.view"), projects = listOf(testData.project.id + 999))
    performGet("/v2/api-keys/current-permissions?projectId=${testData.project.id}", bearer(token))
      .andIsForbidden
  }

  @Test
  fun `requires an explicit project for current-permissions with an OAuth token`() {
    val token = mint(scopes = listOf("translations.view"), projects = OAuth2Constants.ALL_PROJECTS)
    performGet("/v2/api-keys/current-permissions", bearer(token)).andIsBadRequest
  }

  @Test
  fun `rejects an access token issued before the user invalidated their tokens`() {
    val token = mint(scopes = listOf("translations.view"), projects = OAuth2Constants.ALL_PROJECTS)
    performGet(translationsUrl(), bearer(token)).andIsOk
    val user = userAccountService.get(testData.user.id)
    user.tokensValidNotBefore = Date(System.currentTimeMillis() + 3_600_000)
    userAccountService.save(user)
    performGet(translationsUrl(), bearer(token)).andIsUnauthorized
  }

  @Test
  fun `rejects a token that carries no authorization-id claim`() {
    val token =
      mint(scopes = listOf("translations.view"), projects = listOf(testData.project.id), authorizationId = null)
    performGet(translationsUrl(), bearer(token)).andIsUnauthorized
  }

  @Test
  fun `rejects a token whose authorization no longer exists`() {
    val token =
      mint(scopes = listOf("translations.view"), projects = listOf(testData.project.id), authorizationId = "gone")
    performGet(translationsUrl(), bearer(token)).andIsUnauthorized
  }

  @Test
  fun `an admin's OAuth token is bound to real membership, not the admin's server-wide reach`() {
    // Non-member access is masked as 404, same as any stranger.
    val token = mint(scopes = listOf("translations.view"), projects = OAuth2Constants.ALL_PROJECTS, subject = adminUser.id)
    performGet(translationsUrl(), bearer(token)).andIsNotFound
  }

  @Test
  fun `a supporter's OAuth token cannot read a project the supporter is not a member of`() {
    val token =
      mint(scopes = listOf("translations.view"), projects = OAuth2Constants.ALL_PROJECTS, subject = supporterUser.id)
    performGet(translationsUrl(), bearer(token)).andIsNotFound
  }

  @Test
  fun `an admin's OAuth token honors the user's per-language translate restriction`() {
    // The admin is a TRANSLATE member limited to German; the per-language check must run for the OAuth token (the admin
    // bypass is suppressed), so editing English is denied even though the token covers the project and the edit scope.
    val token =
      mint(scopes = listOf("translations.edit"), projects = OAuth2Constants.ALL_PROJECTS, subject = langRestrictedAdmin.id)
    setTranslation("en", token).andIsForbidden
    setTranslation("de", token).andIsOk
  }

  private fun setTranslation(
    languageTag: String,
    token: String,
  ) = performPut(
    "/v2/projects/${testData.project.id}/translations",
    mapOf("key" to "oauth-own-comment-key", "translations" to mapOf(languageTag to "value")),
    bearer(token),
  )

  @Test
  fun `rejects a token whose subject user no longer exists`() {
    // A deleted account keeps its authorization row (delete doesn't revoke grants), so liveness still passes; the
    // resolver must reject once findDto returns null for the subject, or a soft-deleted user keeps a working token.
    val token = mint(scopes = listOf("translations.view"), projects = listOf(testData.project.id), subject = 9_999_999L)
    performGet(translationsUrl(), bearer(token)).andIsUnauthorized
  }

  @Test
  fun `the own-jobs fallback cannot widen a token that lacks batch-jobs-view`() {
    val currentJobs = "/v2/projects/${testData.project.id}/current-batch-jobs"
    val viewOnly = mint(scopes = listOf("translations.view"), projects = listOf(testData.project.id))
    performGet(currentJobs, bearer(viewOnly)).andIsForbidden
    val withBatch =
      mint(scopes = listOf("translations.view", "batch-jobs.view"), projects = listOf(testData.project.id))
    performGet(currentJobs, bearer(withBatch)).andIsOk
  }

  @Test
  fun `the own-comment fallback cannot widen a token that lacks comments-edit`() {
    val commentUrl =
      "/v2/projects/${testData.project.id}/translations/${ownCommentTranslation.id}/comments/${ownComment.id}"
    val token = mint(scopes = listOf("translations.view"), projects = listOf(testData.project.id))
    performPut(commentUrl, TranslationCommentDto(text = "edited"), bearer(token)).andIsForbidden
    performDelete(commentUrl, null, bearer(token)).andIsForbidden

    val editToken =
      mint(scopes = listOf("translations.view", "translation-comments.edit"), projects = listOf(testData.project.id))
    performPut(commentUrl, TranslationCommentDto(text = "edited"), bearer(editToken)).andIsOk
    performDelete(commentUrl, null, bearer(editToken)).andIsOk
  }

  @Test
  fun `the own-batch-job fallback cannot widen a token that lacks batch-jobs scopes`() {
    val jobUrl = "/v2/projects/${testData.project.id}/batch-jobs/${ownBatchJob.id}"
    val token = mint(scopes = listOf("translations.view"), projects = listOf(testData.project.id))
    performGet(jobUrl, bearer(token)).andIsForbidden
    performPut("$jobUrl/cancel", null, bearer(token)).andIsForbidden

    val viewToken =
      mint(scopes = listOf("translations.view", "batch-jobs.view"), projects = listOf(testData.project.id))
    performGet(jobUrl, bearer(viewToken)).andIsOk
  }

  private fun translationsUrl() = "/v2/projects/${testData.project.id}/translations"

  private fun bearer(token: String) = HttpHeaders().apply { add(HttpHeaders.AUTHORIZATION, "Bearer $token") }

  private fun mint(
    scopes: List<String>,
    projects: Any,
    audience: String = apiAudience,
    subject: Long = testData.user.id,
    authorizationId: String? = LIVE_AUTHORIZATION_ID,
  ): String {
    val now = Instant.now()
    val claims =
      JwtClaimsSet
        .builder()
        .subject(subject.toString())
        .audience(listOf(audience))
        .issuedAt(now)
        .expiresAt(now.plus(30, ChronoUnit.MINUTES))
        .claim("scope", scopes)
        .claim(OAuth2Constants.PROJECTS_CLAIM, projects)
        .apply { authorizationId?.let { claim(OAuth2Constants.AUTHORIZATION_ID_CLAIM, it) } }
        .build()
    val header = JwsHeader.with(SignatureAlgorithm.RS256).build()
    return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).tokenValue
  }

  companion object {
    private const val LIVE_AUTHORIZATION_ID = "oauth-access-token-auth-test-authz"
  }

  private val apiAudience: String
    get() =
      tolgeeProperties.backEndUrl
        ?: tolgeeProperties.frontEndUrl
        ?: OAuth2AudienceResolver.DEFAULT_API_AUDIENCE
}
