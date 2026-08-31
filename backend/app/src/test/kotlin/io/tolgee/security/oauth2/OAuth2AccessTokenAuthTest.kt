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

import io.tolgee.component.KeyGenerator
import io.tolgee.development.testDataBuilder.data.BaseTestData
import io.tolgee.dtos.request.translation.comment.TranslationCommentDto
import io.tolgee.fixtures.OAuth2TestTokens
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsNotFound
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.andIsUnauthorized
import io.tolgee.fixtures.bearerHeaders
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.model.batch.BatchJob
import io.tolgee.model.enums.ProjectPermissionType
import io.tolgee.model.translation.Translation
import io.tolgee.model.translation.TranslationComment
import io.tolgee.repository.oauth2.OAuth2GrantRepository
import io.tolgee.testing.AbstractControllerTest
import io.tolgee.testing.assert
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.test.web.servlet.MvcResult
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.zip.ZipInputStream

/**
 * Verifies that an OAuth bearer token is resolved as one, and that SecurityService intersects its scope set and its
 * project set with the user's live permissions.
 *
 * Access tokens are opaque, so a token here is an authorization row written straight into the store rather than
 * anything self-describing; the authorization-code dance is covered by OAuth2AuthorizationCodeFlowTest.
 */
class OAuth2AccessTokenAuthTest : AbstractControllerTest() {
  @Autowired
  private lateinit var grantRepository: OAuth2GrantRepository

  @Autowired
  private lateinit var keyGenerator: KeyGenerator

  private lateinit var tokens: OAuth2TestTokens

  private lateinit var testData: BaseTestData
  private lateinit var viewOnlyUser: UserAccount
  private lateinit var adminUser: UserAccount
  private lateinit var supporterUser: UserAccount
  private lateinit var langRestrictedAdmin: UserAccount
  private lateinit var viewRestrictedAdmin: UserAccount
  private lateinit var ownComment: TranslationComment
  private lateinit var ownCommentTranslation: Translation
  private lateinit var ownBatchJob: BatchJob
  private lateinit var foreignProject: Project

  @BeforeEach
  fun setup() {
    testData = BaseTestData()
    viewOnlyUser = testData.root.addUserAccount { username = "oauth_view_only_user" }.self
    adminUser =
      testData.root
        .addUserAccount {
          username = "oauth_admin_user"
          role = UserAccount.Role.ADMIN
        }.self
    supporterUser =
      testData.root
        .addUserAccount {
          username = "oauth_supporter_user"
          role = UserAccount.Role.SUPPORTER
        }.self
    val german =
      testData.projectBuilder
        .addLanguage {
          name = "German"
          tag = "de"
          originalName = "German"
        }.self
    langRestrictedAdmin =
      testData.root
        .addUserAccount {
          username = "oauth_lang_admin"
          role = UserAccount.Role.ADMIN
        }.self
    testData.projectBuilder.addPermission {
      user = langRestrictedAdmin
      type = ProjectPermissionType.TRANSLATE
      translateLanguages = mutableSetOf(german)
    }
    testData.projectBuilder.addPermission {
      user = viewOnlyUser
      type = ProjectPermissionType.VIEW
    }
    viewRestrictedAdmin =
      testData.root
        .addUserAccount {
          username = "oauth_view_lang_admin"
          role = UserAccount.Role.ADMIN
        }.self
    testData.projectBuilder.addPermission {
      user = viewRestrictedAdmin
      type = ProjectPermissionType.VIEW
      viewLanguages = mutableSetOf(german)
    }
    testData.projectBuilder
      .addKey { name = "oauth-own-comment-key" }
      .build {
        addTranslation {
          language = german
          text = "Wert"
        }
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
    foreignProject =
      testData.root
        .addProject {
          name = FOREIGN_PROJECT_NAME
          organizationOwner = testData.projectBuilder.self.organizationOwner
        }.build {
          addLanguage {
            name = "English"
            tag = "en"
          }
        }.self
    ownBatchJob =
      testData.projectBuilder
        .addBatchJob {
          author = testData.user
          totalItems = 1
        }.self
    testDataService.saveTestData(testData.root)
    tokens = OAuth2TestTokens(grantRepository, userAccountService, keyGenerator)
  }

  @AfterEach
  fun cleanup() {
    tokens.deleteAll()
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `a scope refusal answers a bearer caller with insufficient_scope`() {
    val token = mint(scopes = listOf("translations.view"), projects = listOf(testData.project.id))
    assertThat(performGet(translationsUrl(), bearerHeaders(token)).andReturn().response.status).isEqualTo(200)

    val forbidden =
      performPut(
        translationsUrl(),
        mapOf("key" to "oauth-own-comment-key", "translations" to mapOf("en" to "x")),
        bearerHeaders(token),
      ).andReturn().response

    forbidden.status.assert.isEqualTo(403)
    forbidden
      .getHeader("WWW-Authenticate")
      .assert
      .isNotNull()
      .contains("insufficient_scope")
  }

  @Test
  fun `accepts a valid scoped token`() {
    val token = mint(scopes = listOf("translations.view"), projects = null)
    performGet(translationsUrl(), bearerHeaders(token)).andIsOk
  }

  @Test
  fun `is forbidden on endpoints not opened to any API token`() {
    val token = mint(scopes = listOf("translations.view"), projects = null)
    performGet("/v2/projects", bearerHeaders(token)).andIsForbidden
  }

  @Test
  fun `reaches the non-project endpoints every other API credential reaches`() {
    // An OAuth token is gated by @AllowApiAccess.tokenType alone, exactly as a project API key is. These endpoints
    // are account-level and apply no scope narrowing to any credential - a project API key reads them today for the
    // same reason. Whether they should be ONLY_PAT is a question about all API credentials, not about OAuth.
    val token = mint(scopes = listOf("translations.view"), projects = listOf(testData.project.id))

    performGet("/v2/user", bearerHeaders(token)).andIsOk
    performGet("/v2/notification", bearerHeaders(token)).andIsOk
    performGet("/v2/notification-settings", bearerHeaders(token)).andIsOk
    performGet("/v2/user-tasks", bearerHeaders(token)).andIsOk
  }

  @Test
  fun `cannot exceed the user's live permissions`() {
    val token =
      mint(scopes = listOf("members.view"), projects = null, subject = viewOnlyUser.id)
    performGet("/v2/projects/${testData.project.id}/users", bearerHeaders(token)).andIsForbidden
  }

  @Test
  fun `fails closed on an unparseable stored project selection`() {
    val token = mint(scopes = listOf("translations.view"), projects = listOf(testData.project.id))
    tokens.corruptProjectSelection(token, "nonsense")

    performGet(translationsUrl(), bearerHeaders(token)).andIsNotFound
  }

  @Test
  fun `grants access to a project inside the token project set`() {
    val token = mint(scopes = listOf("translations.view"), projects = listOf(testData.project.id))
    performGet(translationsUrl(), bearerHeaders(token)).andIsOk
  }

  @Test
  fun `hides a project outside the token project set`() {
    val token = mint(scopes = listOf("translations.view"), projects = listOf(testData.project.id + 999))
    performGet(translationsUrl(), bearerHeaders(token)).andIsNotFound
  }

  @Test
  fun `narrows scopes below the user's live permissions`() {
    val token = mint(scopes = listOf("members.view"), projects = null)
    performGet(translationsUrl(), bearerHeaders(token)).andIsForbidden
  }

  @Test
  fun `serves current-permissions for an OAuth token`() {
    val token = mint(scopes = listOf("translations.view"), projects = null)
    performGet("/v2/api-keys/current-permissions?projectId=${testData.project.id}", bearerHeaders(token))
      .andIsOk
      .andAssertThatJson {
        node("projectId").isNumber
        node("type").isNull()
        node("scopes").isArray.contains("translations.view").doesNotContain("keys.edit", "admin")
      }
  }

  @Test
  fun `denies current-permissions for a project outside the token project set`() {
    val token = mint(scopes = listOf("translations.view"), projects = listOf(testData.project.id + 999))
    performGet("/v2/api-keys/current-permissions?projectId=${testData.project.id}", bearerHeaders(token))
      .andIsNotFound
  }

  @Test
  fun `an all-projects token cannot read the details of a project its user has no access to`() {
    val token = mint(scopes = listOf("translations.view"), projects = null, subject = viewOnlyUser.id)

    val response =
      performGet(
        "/v2/api-keys/current-permissions?projectId=${foreignProject.id}",
        bearerHeaders(token),
      ).andIsNotFound.andReturn().response

    response.contentAsString.assert.doesNotContain(FOREIGN_PROJECT_NAME)
  }

  @Test
  fun `requires an explicit project for current-permissions with an all-projects token`() {
    val token = mint(scopes = listOf("translations.view"), projects = null)
    performGet("/v2/api-keys/current-permissions", bearerHeaders(token)).andIsBadRequest
  }

  @Test
  fun `serves current-permissions without a projectId when the token is bound to one project`() {
    val token = mint(scopes = listOf("translations.view"), projects = listOf(testData.project.id))

    performGet("/v2/api-keys/current-permissions", bearerHeaders(token))
      .andIsOk
      .andAssertThatJson {
        node("projectId").isEqualTo(testData.project.id)
        node("scopes").isArray.contains("translations.view")
      }
  }

  @Test
  fun `requires an explicit project for current-permissions with a multi-project token`() {
    val token =
      mint(scopes = listOf("translations.view"), projects = listOf(testData.project.id, foreignProject.id))
    performGet("/v2/api-keys/current-permissions", bearerHeaders(token)).andIsBadRequest
  }

  @Test
  fun `rejects an access token issued before the user invalidated their tokens`() {
    val token = mint(scopes = listOf("translations.view"), projects = null)
    performGet(translationsUrl(), bearerHeaders(token)).andIsOk
    val user = userAccountService.get(testData.user.id)
    user.tokensValidNotBefore = Date(System.currentTimeMillis() + 3_600_000)
    userAccountService.save(user)
    performGet(translationsUrl(), bearerHeaders(token)).andIsUnauthorized
  }

  @Test
  fun `a revoked token stops working at once`() {
    val token = mint(scopes = listOf("translations.view"), projects = listOf(testData.project.id))
    performGet(translationsUrl(), bearerHeaders(token)).andIsOk

    tokens.revoke(token)

    performGet(translationsUrl(), bearerHeaders(token)).andIsUnauthorized
  }

  @Test
  fun `rejects a token whose client is no longer registered`() {
    val token =
      tokens.issue(
        subject = testData.user.id,
        scopes = listOf("translations.view"),
        projectIds = listOf(testData.project.id),
        clientId = "no-longer-registered-client",
      )

    performGet(translationsUrl(), bearerHeaders(token)).andIsUnauthorized
  }

  @Test
  fun `rejects an opaque token that was never issued`() {
    performGet(translationsUrl(), bearerHeaders("test-never-issued-token")).andIsUnauthorized
  }

  @Test
  fun `rejects an expired token`() {
    val token =
      mint(
        scopes = listOf("translations.view"),
        projects = listOf(testData.project.id),
        issuedAt = Instant.now().minus(2, ChronoUnit.HOURS),
        expiresAt = Instant.now().minus(1, ChronoUnit.HOURS),
      )
    performGet(translationsUrl(), bearerHeaders(token)).andIsUnauthorized
  }

  @Test
  fun `an admin's OAuth token is bound to real membership, not the admin's server-wide reach`() {
    val token =
      mint(scopes = listOf("translations.view"), projects = null, subject = adminUser.id)
    performGet(translationsUrl(), bearerHeaders(token)).andIsNotFound
  }

  @Test
  fun `a supporter's OAuth token cannot read a project the supporter is not a member of`() {
    val token =
      mint(scopes = listOf("translations.view"), projects = null, subject = supporterUser.id)
    performGet(translationsUrl(), bearerHeaders(token)).andIsNotFound
  }

  @Test
  fun `an admin's OAuth token honors the user's per-language translate restriction`() {
    val token =
      mint(
        scopes = listOf("translations.edit"),
        projects = null,
        subject = langRestrictedAdmin.id,
      )
    setTranslation("en", token).andIsForbidden
    setTranslation("de", token).andIsOk
  }

  @Test
  fun `an admin's OAuth token sees only its permitted languages when the list enumerates them`() {
    val token =
      mint(
        scopes = listOf("translations.view"),
        projects = null,
        subject = viewRestrictedAdmin.id,
      )

    performGet(translationsUrl(), bearerHeaders(token))
      .andIsOk
      .andAssertThatJson {
        node("_embedded.keys[0].translations").isObject.containsKey("de")
        node("_embedded.keys[0].translations").isObject.doesNotContainKey("en")
      }
  }

  @Test
  fun `an admin's OAuth token cannot name a hidden language through the explicit languages filter`() {
    val token =
      mint(
        scopes = listOf("translations.view"),
        projects = null,
        subject = viewRestrictedAdmin.id,
      )

    performGet(translationsUrl() + "?languages=en", bearerHeaders(token))
      .andIsOk
      .andAssertThatJson {
        node("_embedded.keys[0].translations").isObject.doesNotContainKey("en")
      }
  }

  @Test
  fun `an admin's OAuth token exports only its permitted languages`() {
    val token =
      mint(
        scopes = listOf("translations.view"),
        projects = null,
        subject = viewRestrictedAdmin.id,
      )

    val bytes =
      performGet("/v2/projects/${testData.project.id}/export", bearerHeaders(token))
        .andIsOk
        .andDo { obj: MvcResult -> obj.asyncResult }
        .andReturn()
        .response.contentAsByteArray

    val entries = mutableListOf<String>()
    ZipInputStream(bytes.inputStream()).use { zip ->
      var entry = zip.nextEntry
      while (entry != null) {
        entries.add(entry.name)
        entry = zip.nextEntry
      }
    }
    entries.assert.containsExactly("de.json")
  }

  @Test
  fun `a token stops working once its subject account is deleted`() {
    val token =
      mint(
        scopes = listOf("translations.view"),
        projects = listOf(testData.project.id),
        subject = viewOnlyUser.id,
      )
    performGet(translationsUrl(), bearerHeaders(token)).andIsOk

    userAccountService.delete(userAccountService.get(viewOnlyUser.id))

    performGet(translationsUrl(), bearerHeaders(token)).andIsUnauthorized
  }

  @Test
  fun `the own-jobs fallback cannot widen a token that lacks batch-jobs-view`() {
    val currentJobs = "/v2/projects/${testData.project.id}/current-batch-jobs"
    val viewOnly = mint(scopes = listOf("translations.view"), projects = listOf(testData.project.id))
    performGet(currentJobs, bearerHeaders(viewOnly)).andIsForbidden
    val withBatch =
      mint(scopes = listOf("translations.view", "batch-jobs.view"), projects = listOf(testData.project.id))
    performGet(currentJobs, bearerHeaders(withBatch)).andIsOk
  }

  @Test
  fun `the own-comment fallback cannot widen a token that lacks comments-edit`() {
    val commentUrl =
      "/v2/projects/${testData.project.id}/translations/${ownCommentTranslation.id}/comments/${ownComment.id}"
    val token = mint(scopes = listOf("translations.view"), projects = listOf(testData.project.id))
    performPut(commentUrl, TranslationCommentDto(text = "edited"), bearerHeaders(token)).andIsForbidden
    performDelete(commentUrl, null, bearerHeaders(token)).andIsForbidden

    val editToken =
      mint(
        scopes = listOf("translations.view", "translation-comments.edit"),
        projects = listOf(testData.project.id),
      )
    performPut(commentUrl, TranslationCommentDto(text = "edited"), bearerHeaders(editToken)).andIsOk
    performDelete(commentUrl, null, bearerHeaders(editToken)).andIsOk
  }

  @Test
  fun `the own-batch-job fallback cannot widen a token that lacks batch-jobs scopes`() {
    val jobUrl = "/v2/projects/${testData.project.id}/batch-jobs/${ownBatchJob.id}"
    val token = mint(scopes = listOf("translations.view"), projects = listOf(testData.project.id))
    performGet(jobUrl, bearerHeaders(token)).andIsForbidden
    performPut("$jobUrl/cancel", null, bearerHeaders(token)).andIsForbidden

    val viewToken =
      mint(scopes = listOf("translations.view", "batch-jobs.view"), projects = listOf(testData.project.id))
    performGet(jobUrl, bearerHeaders(viewToken)).andIsOk
  }

  private fun setTranslation(
    languageTag: String,
    token: String,
  ) = performPut(
    "/v2/projects/${testData.project.id}/translations",
    mapOf("key" to "oauth-own-comment-key", "translations" to mapOf(languageTag to "value")),
    bearerHeaders(token),
  )

  private fun translationsUrl() = "/v2/projects/${testData.project.id}/translations"

  companion object {
    private const val FOREIGN_PROJECT_NAME = "oauth-foreign-project"
  }

  private fun mint(
    scopes: List<String>,
    projects: Collection<Long>?,
    subject: Long = testData.user.id,
    issuedAt: Instant = Instant.now(),
    expiresAt: Instant = issuedAt.plus(30, ChronoUnit.MINUTES),
  ): String =
    tokens.issue(
      subject = subject,
      scopes = scopes,
      projectIds = projects,
      issuedAt = issuedAt,
      expiresAt = expiresAt,
    )
}
