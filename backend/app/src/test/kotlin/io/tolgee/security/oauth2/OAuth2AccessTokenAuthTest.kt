package io.tolgee.security.oauth2

import io.tolgee.component.KeyGenerator
import io.tolgee.development.testDataBuilder.data.OAuth2AccessTokenAuthTestData
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
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
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

  private lateinit var testData: OAuth2AccessTokenAuthTestData

  @BeforeEach
  fun setup() {
    testData = OAuth2AccessTokenAuthTestData()
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
    performGet(translationsUrl(), bearerHeaders(token))
      .andReturn()
      .response.status.assert
      .isEqualTo(200)

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
    val token = mintForAllProjects(scopes = listOf("translations.view"))
    performGet(translationsUrl(), bearerHeaders(token)).andIsOk
  }

  @Test
  fun `is forbidden on endpoints not opened to any API token`() {
    val token = mintForAllProjects(scopes = listOf("translations.view"))
    performGet("/v2/projects", bearerHeaders(token)).andIsForbidden
  }

  @Test
  fun `reaches the non-project endpoints every other API credential reaches`() {
    val token = mint(scopes = listOf("translations.view"), projects = listOf(testData.project.id))

    performGet("/v2/user", bearerHeaders(token)).andIsOk
    performGet("/v2/notification", bearerHeaders(token)).andIsOk
    performGet("/v2/notification-settings", bearerHeaders(token)).andIsOk
    performGet("/v2/user-tasks", bearerHeaders(token)).andIsOk
  }

  @Test
  fun `cannot exceed the user's live permissions`() {
    val token =
      mintForAllProjects(scopes = listOf("members.view"), subject = testData.viewOnlyUser.id)
    performGet("/v2/projects/${testData.project.id}/users", bearerHeaders(token)).andIsForbidden
  }

  @Test
  fun `fails closed on an unparseable stored project selection`() {
    val token = mint(scopes = listOf("translations.view"), projects = listOf(testData.project.id))
    tokens.corruptProjectSelection(token, "nonsense")

    performGet(translationsUrl(), bearerHeaders(token)).andIsForbidden
  }

  @Test
  fun `grants access to a project inside the token project set`() {
    val token = mint(scopes = listOf("translations.view"), projects = listOf(testData.project.id))
    performGet(translationsUrl(), bearerHeaders(token)).andIsOk
  }

  @Test
  fun `refuses a project outside the token project set`() {
    val token = mint(scopes = listOf("translations.view"), projects = listOf(testData.project.id + 999))
    performGet(translationsUrl(), bearerHeaders(token)).andIsForbidden
  }

  @Test
  fun `narrows scopes below the user's live permissions`() {
    val token = mintForAllProjects(scopes = listOf("members.view"))
    performGet(translationsUrl(), bearerHeaders(token)).andIsForbidden
  }

  @Test
  fun `serves current-permissions for an OAuth token`() {
    val token = mintForAllProjects(scopes = listOf("translations.view"))
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
    val token = mintForAllProjects(scopes = listOf("translations.view"), subject = testData.viewOnlyUser.id)

    val response =
      performGet(
        "/v2/api-keys/current-permissions?projectId=${testData.foreignProject.id}",
        bearerHeaders(token),
      ).andIsNotFound.andReturn().response

    response.contentAsString.assert.doesNotContain(OAuth2AccessTokenAuthTestData.FOREIGN_PROJECT_NAME)
  }

  @Test
  fun `requires an explicit project for current-permissions with an all-projects token`() {
    val token = mintForAllProjects(scopes = listOf("translations.view"))
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
      mint(scopes = listOf("translations.view"), projects = listOf(testData.project.id, testData.foreignProject.id))
    performGet("/v2/api-keys/current-permissions", bearerHeaders(token)).andIsBadRequest
  }

  @Test
  fun `a project the user cannot see stays hidden, so the token is no project-existence oracle`() {
    val token = mint(scopes = listOf("translations.view"), projects = listOf(testData.strangersProject.id))

    performGet("/v2/projects/${testData.strangersProject.id}/translations", bearerHeaders(token)).andIsNotFound
  }

  @Test
  fun `rejects an access token issued before the user invalidated their tokens`() {
    val token = mintForAllProjects(scopes = listOf("translations.view"))
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
      mintForAllProjects(scopes = listOf("translations.view"), subject = testData.adminUser.id)
    performGet(translationsUrl(), bearerHeaders(token)).andIsNotFound
  }

  @Test
  fun `a supporter's OAuth token cannot read a project the supporter is not a member of`() {
    val token =
      mintForAllProjects(scopes = listOf("translations.view"), subject = testData.supporterUser.id)
    performGet(translationsUrl(), bearerHeaders(token)).andIsNotFound
  }

  @Test
  fun `an admin's OAuth token honors the user's per-language translate restriction`() {
    val token =
      mintForAllProjects(
        scopes = listOf("translations.edit"),
        subject = testData.langRestrictedAdmin.id,
      )
    setTranslation("en", token).andIsForbidden
    setTranslation("de", token).andIsOk
  }

  @Test
  fun `an admin's OAuth token sees only its permitted languages when the list enumerates them`() {
    val token =
      mintForAllProjects(
        scopes = listOf("translations.view"),
        subject = testData.viewRestrictedAdmin.id,
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
      mintForAllProjects(
        scopes = listOf("translations.view"),
        subject = testData.viewRestrictedAdmin.id,
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
      mintForAllProjects(
        scopes = listOf("translations.view"),
        subject = testData.viewRestrictedAdmin.id,
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
        subject = testData.viewOnlyUser.id,
      )
    performGet(translationsUrl(), bearerHeaders(token)).andIsOk

    userAccountService.delete(userAccountService.get(testData.viewOnlyUser.id))

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
      "/v2/projects/${testData.project.id}/translations/${testData.ownCommentTranslation.id}/comments/${testData.ownComment.id}"
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
    val jobUrl = "/v2/projects/${testData.project.id}/batch-jobs/${testData.ownBatchJob.id}"
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

  /** A token the consent screen bound to every project the user can reach, not to none. */
  private fun mintForAllProjects(
    scopes: List<String>,
    subject: Long = testData.user.id,
  ): String = mint(scopes = scopes, projects = null, subject = subject)

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
