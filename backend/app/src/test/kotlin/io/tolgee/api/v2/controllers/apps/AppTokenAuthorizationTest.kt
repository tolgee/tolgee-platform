package io.tolgee.api.v2.controllers.apps

import io.tolgee.constants.Message
import io.tolgee.development.testDataBuilder.data.AppsTestData
import io.tolgee.dtos.request.organization.SetOrganizationRoleDto
import io.tolgee.fixtures.andHasErrorMessage
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.andIsUnauthorized
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.OrganizationRoleType
import io.tolgee.security.authentication.AppTokenService
import io.tolgee.service.apps.AppManifestHttpClient
import io.tolgee.service.apps.AppsTestFixtures
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import tools.jackson.databind.JsonNode
import java.util.Date

/**
 * Covers what an app token may and may not reach. The install below is granted only
 * `translations.edit`, while the person who registered it is the organization owner — so anything
 * the token reaches beyond that grant is that person's privileges leaking through.
 */
class AppTokenAuthorizationTest : AuthorizedControllerTest() {
  @Autowired
  lateinit var appTokenService: AppTokenService

  @MockitoBean
  @Autowired
  lateinit var appManifestHttpClient: AppManifestHttpClient

  lateinit var testData: AppsTestData
  var installId: Long = 0
  lateinit var installToken: String

  @BeforeEach
  fun setup() {
    testData = AppsTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    AppsTestFixtures.mockManifest(appManifestHttpClient, MANIFEST)

    val registration =
      performAuthPost(
        "/v2/organizations/${testData.organization.id}/owned-apps",
        mapOf("manifestUrl" to AppsTestFixtures.MANIFEST_URL),
      ).andIsOk.andReturn().response.contentAsString
    val json = objectMapper.readTree(registration)
    installId = json.get("installId").asLong()

    performAuthPut("/v2/projects/${testData.project.id}/apps/$installId", null).andIsOk
    installToken = requestInstallToken(json)
  }

  @AfterEach
  fun cleanup() {
    currentDateProvider.forcedDate = null
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `reaches a project endpoint within the install's granted scopes`() {
    asApp(get("/v2/projects/${testData.project.id}/translations")).andIsOk
  }

  @Test
  fun `is rejected on the current-user endpoint`() {
    asApp(get("/v2/user")).andIsForbidden.andHasErrorMessage(Message.APP_ACCESS_FORBIDDEN)
  }

  /**
   * Minting is the one app-management route with no scope gate in front of it, so nothing but the
   * app-access rule stops an app from minting a token for an install other than its own.
   */
  @Test
  fun `is rejected when minting a token for an install`() {
    asApp(post("/v2/projects/${testData.project.id}/apps/$installId/token"))
      .andIsForbidden
      .andHasErrorMessage(Message.APP_ACCESS_FORBIDDEN)
  }

  /**
   * An install whose manifest asks for apps.manage clears the scope check that guards the
   * app-management endpoints, so only the app-access rule keeps it from managing apps for the
   * project.
   */
  @Test
  fun `cannot manage apps even when its grant covers apps manage`() {
    AppsTestFixtures.mockManifest(appManifestHttpClient, APPS_MANAGE_MANIFEST)
    val registration =
      performAuthPost(
        "/v2/organizations/${testData.organization.id}/owned-apps",
        mapOf("manifestUrl" to AppsTestFixtures.MANIFEST_URL),
      ).andIsOk.andReturn().response.contentAsString
    val json = objectMapper.readTree(registration)
    val privilegedInstallId = json.get("installId").asLong()
    performAuthPut("/v2/projects/${testData.project.id}/apps/$privilegedInstallId", null).andIsOk
    val privilegedToken = requestInstallToken(json)

    asToken(privilegedToken, get("/v2/projects/${testData.project.id}/apps"))
      .andIsForbidden
      .andHasErrorMessage(Message.APP_ACCESS_FORBIDDEN)

    asToken(privilegedToken, put("/v2/projects/${testData.project.id}/apps/$installId"))
      .andIsForbidden
      .andHasErrorMessage(Message.APP_ACCESS_FORBIDDEN)

    asToken(privilegedToken, delete("/v2/projects/${testData.project.id}/apps/$installId"))
      .andIsForbidden
      .andHasErrorMessage(Message.APP_ACCESS_FORBIDDEN)
  }

  @Test
  fun `is rejected on a legacy api project route for a project it is not enabled for`() {
    // An existing-but-not-enabled project is indistinguishable from a nonexistent one, so an app
    // cannot enumerate project ids across tenants.
    asApp(get("/api/project/${testData.siblingProject.id}/export/jsonZip"))
      .andIsForbidden
      .andHasErrorMessage(Message.APP_ACCESS_FORBIDDEN)
  }

  @Test
  fun `is rejected on a v2 project route for a project it is not enabled for`() {
    asApp(get("/v2/projects/${testData.siblingProject.id}/translations"))
      .andIsForbidden
      .andHasErrorMessage(Message.APP_ACCESS_FORBIDDEN)
  }

  @Test
  fun `denies a scope outside the grant that is only checked in the handler body`() {
    asApp(
      post("/v2/projects/${testData.project.id}/translations")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""{"key":"brand-new-key","translations":{"en":"Hello"}}"""),
    ).andIsForbidden.andHasErrorMessage(Message.OPERATION_NOT_PERMITTED)
  }

  /** The install belongs to the organization; the person who registered it may have left. */
  @Test
  fun `keeps working once its author has been disabled`() {
    userAccountService.disable(testData.user.id)
    asApp(get("/v2/projects/${testData.project.id}/translations")).andIsOk
  }

  @Test
  fun `keeps working once its author has been deleted`() {
    // Somebody else has to own the organization first, or deleting its only owner takes it with them.
    organizationRoleService.setMemberRole(
      testData.organization.id,
      testData.member.id,
      SetOrganizationRoleDto(OrganizationRoleType.OWNER),
    )
    userAccountService.delete(testData.user.id)

    asApp(get("/v2/projects/${testData.project.id}/translations")).andIsOk
  }

  /**
   * The author's server role must not reach the install, whatever it is — neither to reach a project
   * the app was never enabled for, nor to bypass a scope check inside one it was.
   */
  @Test
  fun `does not gain the author's server-admin privileges`() {
    val author = userAccountService.get(testData.user.id)
    author.role = UserAccount.Role.ADMIN
    userAccountService.save(author)

    asApp(get("/v2/projects/${testData.siblingProject.id}/translations"))
      .andIsForbidden
      .andHasErrorMessage(Message.APP_ACCESS_FORBIDDEN)

    asApp(
      post("/v2/projects/${testData.project.id}/translations")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""{"key":"brand-new-key","translations":{"en":"Hello"}}"""),
    ).andIsForbidden.andHasErrorMessage(Message.OPERATION_NOT_PERMITTED)
  }

  @Test
  fun `reports an expired token as expired rather than invalid`() {
    currentDateProvider.forcedDate =
      Date(currentDateProvider.date.time + tolgeeProperties.apps.tokenExpiration + 10_000)

    asApp(get("/v2/projects/${testData.project.id}/translations"))
      .andIsUnauthorized
      .andHasErrorMessage(Message.EXPIRED_JWT_TOKEN)
  }

  @Test
  fun `honours the read-only flag carried by a user-context token`() {
    val readOnlyToken =
      appTokenService.mintUserContextToken(
        installId = installId,
        userId = testData.user.id,
        projectId = testData.project.id,
        isReadOnly = true,
      )

    asToken(
      readOnlyToken,
      post("/v2/projects/${testData.project.id}/translations")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""{"key":"brand-new-key","translations":{"en":"Hello"}}"""),
    ).andIsForbidden.andHasErrorMessage(Message.OPERATION_NOT_PERMITTED_IN_READ_ONLY_MODE)
  }

  @Test
  fun `mints a read-write user-context token for a read-write session`() {
    val response =
      performAuthPost("/v2/projects/${testData.project.id}/apps/$installId/token", null)
        .andIsOk
        .andReturn()
        .response.contentAsString
    val token = objectMapper.readTree(response).get("token").asText()

    appTokenService
      .validateToken(token)
      .isReadOnly.assert
      .isEqualTo(false)
  }

  private fun asApp(builder: MockHttpServletRequestBuilder): ResultActions = asToken(installToken, builder)

  private fun asToken(
    token: String,
    builder: MockHttpServletRequestBuilder,
  ): ResultActions {
    logout()
    return perform(builder.header(HttpHeaders.AUTHORIZATION, "Bearer $token"))
  }

  private fun requestInstallToken(registration: JsonNode): String {
    val response =
      perform(
        post("/v2/public/apps/token")
          .contentType(MediaType.APPLICATION_JSON)
          .content(
            objectMapper.writeValueAsString(
              mapOf(
                "grant_type" to "client_credentials",
                "client_id" to registration.get("clientId").asText(),
                "client_secret" to registration.get("clientSecret").asText(),
                "install_id" to registration.get("installId").asLong(),
              ),
            ),
          ),
      ).andIsOk.andReturn().response.contentAsString
    return objectMapper.readTree(response).get("access_token").asText()
  }

  companion object {
    private val MANIFEST: String =
      """
      {
        "id": "test-app",
        "name": "Test App",
        "version": "0.1.0",
        "baseUrl": "https://app.example.com",
        "scopes": ["translations.edit"],
        "modules": {
          "project-dashboard-page": [
            {"key": "home", "title": "Home", "icon": "🏠", "entry": "/"}
          ]
        }
      }
      """.trimIndent()

    private val APPS_MANAGE_MANIFEST: String =
      """
      {
        "id": "privileged-app",
        "name": "Privileged App",
        "version": "0.1.0",
        "baseUrl": "https://privileged.example.com",
        "scopes": ["apps.manage"],
        "modules": {
          "project-dashboard-page": [
            {"key": "home", "title": "Home", "icon": "🏠", "entry": "/"}
          ]
        }
      }
      """.trimIndent()
  }
}
