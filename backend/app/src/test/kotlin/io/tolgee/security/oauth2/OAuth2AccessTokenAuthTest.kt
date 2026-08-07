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
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsNotFound
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.andIsUnauthorized
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.ProjectPermissionType
import io.tolgee.testing.AbstractControllerTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
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

  private lateinit var testData: BaseTestData
  private lateinit var viewOnlyUser: UserAccount

  @BeforeEach
  fun setup() {
    testData = BaseTestData()
    // A second user with view-only project access, to prove a token can't grant more than the user actually has.
    viewOnlyUser = testData.root.addUserAccount { username = "oauth_view_only_user" }.self
    testData.projectBuilder.addPermission {
      user = viewOnlyUser
      type = ProjectPermissionType.VIEW
    }
    testDataService.saveTestData(testData.root)
  }

  @AfterEach
  fun cleanup() {
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
    // A correctly-shaped token (right audience/subject/scopes) signed with a foreign key must fail signature check.
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
    // /v2/projects is @AllowApiAccess(ONLY_PAT); an OAuth token must not reach it (the gate fails closed).
    val token = mint(scopes = listOf("translations.view"), projects = OAuth2Constants.ALL_PROJECTS)
    performGet("/v2/projects", bearer(token)).andIsForbidden
  }

  @Test
  fun `cannot exceed the user's live permissions`() {
    // The view-only user has no members.view; a token that claims it must still be capped to what the user holds.
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

  private fun translationsUrl() = "/v2/projects/${testData.project.id}/translations"

  private fun bearer(token: String) = HttpHeaders().apply { add(HttpHeaders.AUTHORIZATION, "Bearer $token") }

  private fun mint(
    scopes: List<String>,
    projects: Any,
    audience: String = apiAudience,
    subject: Long = testData.user.id,
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
        .build()
    val header = JwsHeader.with(SignatureAlgorithm.RS256).build()
    return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).tokenValue
  }

  private val apiAudience: String
    get() =
      tolgeeProperties.backEndUrl
        ?: tolgeeProperties.frontEndUrl
        ?: OAuth2AudienceResolver.DEFAULT_API_AUDIENCE
}
