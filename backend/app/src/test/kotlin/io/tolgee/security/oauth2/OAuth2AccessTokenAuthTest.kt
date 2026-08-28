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
import io.tolgee.model.UserAccount
import io.tolgee.model.batch.BatchJob
import io.tolgee.model.enums.ProjectPermissionType
import io.tolgee.model.translation.Translation
import io.tolgee.model.translation.TranslationComment
import io.tolgee.repository.oauth2.OAuth2AuthorizationRepository
import io.tolgee.testing.AbstractControllerTest
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
  private lateinit var authorizationRepository: OAuth2AuthorizationRepository

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
    ownBatchJob =
      testData.projectBuilder
        .addBatchJob {
          author = testData.user
          totalItems = 1
        }.self
    testDataService.saveTestData(testData.root)
    tokens = OAuth2TestTokens(authorizationRepository, userAccountService, keyGenerator)
  }

  @AfterEach
  fun cleanup() {
    tokens.deleteAll()
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `accepts a valid scoped token`() {
    val token = mint(scopes = listOf("translations.view"), projects = OAuth2Constants.ALL_PROJECTS)
    performGet(translationsUrl(), bearer(token)).andIsOk
  }

  @Test
  fun `is forbidden on endpoints not opened to any API token`() {
    val token = mint(scopes = listOf("translations.view"), projects = OAuth2Constants.ALL_PROJECTS)
    performGet("/v2/projects", bearer(token)).andIsForbidden
  }

  @Test
  fun `is forbidden on API endpoints that never apply the token's scopes`() {
    // Nothing outside a project-scoped handler applies scope ∩ project set, so a token there would carry the user's
    // whole account: their email and server role, every notification, every task in every project they belong to.
    // These are all @AllowApiAccess endpoints a PAT or PAK may use; an OAuth token must not.
    val token = mint(scopes = listOf("translations.view"), projects = listOf(testData.project.id))

    performGet("/v2/user", bearer(token)).andIsForbidden
    performGet("/v2/notification", bearer(token)).andIsForbidden
    performGet("/v2/notification-settings", bearer(token)).andIsForbidden
    // Declares @UseDefaultPermissions but sits outside the project paths, so the interceptor never runs for it and
    // it would return every task the user has in every project. Annotations are not the question here; the path is.
    performGet("/v2/user-tasks", bearer(token)).andIsForbidden
  }

  @Test
  fun `cannot exceed the user's live permissions`() {
    val token =
      mint(scopes = listOf("members.view"), projects = OAuth2Constants.ALL_PROJECTS, subject = viewOnlyUser.id)
    performGet("/v2/projects/${testData.project.id}/users", bearer(token)).andIsForbidden
  }

  @Test
  fun `fails closed on an unrecognized project claim shape`() {
    val token = mint(scopes = listOf("translations.view"), projects = "nonsense")
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
  fun `a revoked token stops working at once`() {
    val token = mint(scopes = listOf("translations.view"), projects = listOf(testData.project.id))
    performGet(translationsUrl(), bearer(token)).andIsOk

    tokens.revoke(token)

    performGet(translationsUrl(), bearer(token)).andIsUnauthorized
  }

  @Test
  fun `rejects a token whose client is no longer registered`() {
    // Clients come from configuration, and dropping one is the documented way to switch it off. Its grants outlive it
    // in the store and must stop authenticating — with a 401, not a 500.
    val token =
      tokens.issue(
        subject = testData.user.id,
        scopes = listOf("translations.view"),
        projects = listOf(testData.project.id),
        clientId = "no-longer-registered-client",
      )

    performGet(translationsUrl(), bearer(token)).andIsUnauthorized
  }

  @Test
  fun `rejects an opaque token that was never issued`() {
    performGet(translationsUrl(), bearer("test-never-issued-token")).andIsUnauthorized
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
    performGet(translationsUrl(), bearer(token)).andIsUnauthorized
  }

  @Test
  fun `an admin's OAuth token is bound to real membership, not the admin's server-wide reach`() {
    // Non-member access is masked as 404, same as any stranger.
    val token =
      mint(scopes = listOf("translations.view"), projects = OAuth2Constants.ALL_PROJECTS, subject = adminUser.id)
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
    val token =
      mint(
        scopes = listOf("translations.edit"),
        projects = OAuth2Constants.ALL_PROJECTS,
        subject = langRestrictedAdmin.id,
      )
    setTranslation("en", token).andIsForbidden
    setTranslation("de", token).andIsOk
  }

  @Test
  fun `an admin's OAuth token sees only the languages the admin may view`() {
    // Enumeration, not checking: the translations list asks LanguageService which languages the caller may view. Left
    // admin-bypassed it would answer "all of them" for the very token the check path restricts to German.
    val token =
      mint(
        scopes = listOf("translations.view"),
        projects = OAuth2Constants.ALL_PROJECTS,
        subject = viewRestrictedAdmin.id,
      )

    performGet(translationsUrl(), bearer(token))
      .andIsOk
      .andAssertThatJson {
        node("_embedded.keys[0].translations").isObject.containsKey("de")
        node("_embedded.keys[0].translations").isObject.doesNotContainKey("en")
      }
  }

  @Test
  fun `an admin's OAuth token cannot name a language the admin may not view`() {
    // The explicit ?languages= branch is findByTagsAndFilterPermitted, a different call site from the implicit one
    // above. Asking for English by name must not get past the restriction that hides it from the list.
    val token =
      mint(
        scopes = listOf("translations.view"),
        projects = OAuth2Constants.ALL_PROJECTS,
        subject = viewRestrictedAdmin.id,
      )

    performGet(translationsUrl() + "?languages=en", bearer(token))
      .andIsOk
      .andAssertThatJson {
        node("_embedded.keys[0].translations").isObject.doesNotContainKey("en")
      }
  }

  @Test
  fun `an admin's OAuth token exports only the languages the admin may view`() {
    // getLanguagesForExport is the third call site, and the one that returns translation content rather than a list.
    // Asserted on the zip's entries: with the restriction applied there is one file, and it is the German one.
    val token =
      mint(
        scopes = listOf("translations.view"),
        projects = OAuth2Constants.ALL_PROJECTS,
        subject = viewRestrictedAdmin.id,
      )

    val bytes =
      performGet("/v2/projects/${testData.project.id}/export", bearer(token))
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
    assertThat(entries).containsExactly("de.json")
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
  fun `a token stops working once its subject account is deleted`() {
    val token =
      mint(scopes = listOf("translations.view"), projects = listOf(testData.project.id), subject = viewOnlyUser.id)
    performGet(translationsUrl(), bearer(token)).andIsOk

    userAccountService.delete(userAccountService.get(viewOnlyUser.id))

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
    subject: Long = testData.user.id,
    issuedAt: Instant = Instant.now(),
    expiresAt: Instant = issuedAt.plus(30, ChronoUnit.MINUTES),
  ): String =
    tokens.issue(
      subject = subject,
      scopes = scopes,
      projects = projects,
      issuedAt = issuedAt,
      expiresAt = expiresAt,
    )
}
