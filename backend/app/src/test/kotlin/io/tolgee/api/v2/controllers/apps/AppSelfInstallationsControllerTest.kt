package io.tolgee.api.v2.controllers.apps

import io.tolgee.API_KEY_HEADER_NAME
import io.tolgee.constants.Message
import io.tolgee.development.testDataBuilder.data.NativeAppsTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andHasErrorMessage
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsOk
import io.tolgee.model.enums.Scope
import io.tolgee.security.authentication.AppTokenService
import io.tolgee.service.apps.AppInstallService
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post

/**
 * Covers what an app backend learns about itself from `/v2/apps/self/installations` — and, just as
 * importantly, that nothing but an install-context token learns it.
 */
class AppSelfInstallationsControllerTest : AuthorizedControllerTest() {
  @Autowired
  lateinit var appTokenService: AppTokenService

  @Autowired
  lateinit var appInstallService: AppInstallService

  @MockitoBean
  @Autowired
  lateinit var appManifestHttpClient: AppManifestHttpClient

  lateinit var testData: NativeAppsTestData
  var installId: Long = 0
  lateinit var installToken: String

  @BeforeEach
  fun setup() {
    testData = NativeAppsTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    AppsTestFixtures.mockManifest(appManifestHttpClient, MANIFEST)

    val registration = registerOrganizationInstall()
    installId = registration.first
    performAuthPut("/v2/projects/${testData.project.id}/apps/$installId", null).andIsOk
    installToken = requestInstallToken(registration.second, registration.third)
  }

  @AfterEach
  fun cleanup() {
    AppsTestFixtures.removeNativeInstalls(appInstallService)
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `reports its own install with the projects it is enabled for`() {
    asToken(installToken, get(SELF_INSTALLATIONS)).andIsOk.andAssertThatJson {
      node("_embedded.installations").isArray.hasSize(1)
      node("_embedded.installations[0].id").isEqualTo(installId)
      node("_embedded.installations[0].appId").isEqualTo("test-app")
      node("_embedded.installations[0].name").isEqualTo("Test App")
      node("_embedded.installations[0].version").isEqualTo("0.1.0")
      node("_embedded.installations[0].native").isEqualTo(false)
      node("_embedded.installations[0].scopes").isArray.containsExactly("activity.view")
      node("_embedded.installations[0].enabledProjects").isArray.hasSize(1)
      node("_embedded.installations[0].enabledProjects[0].id").isEqualTo(testData.project.id)
      node("_embedded.installations[0].enabledProjects[0].name").isEqualTo(testData.project.name)
      node("_embedded.installations[0].enabledProjects[0].organization.id")
        .isEqualTo(testData.organization.id)
      node("_embedded.installations[0].enabledProjects[0].organization.name")
        .isEqualTo(testData.organization.name)
      node("_embedded.installations[0].enabledProjects[0].organization.slug")
        .isEqualTo(testData.organization.slug)
    }
  }

  @Test
  fun `omits a project the app is not enabled for until it is enabled`() {
    enabledProjectIds().assert.containsExactly(testData.project.id)

    // Calling as the app logs the session out, and the getter would otherwise fall back to the
    // server's initial user, who cannot see this organization's projects.
    userAccount = testData.user
    performAuthPut("/v2/projects/${testData.siblingProject.id}/apps/$installId", null).andIsOk

    enabledProjectIds().assert.containsExactlyInAnyOrder(
      testData.project.id,
      testData.siblingProject.id,
    )
  }

  @Test
  fun `drops a project once the app is disabled for it`() {
    performAuthDelete("/v2/projects/${testData.project.id}/apps/$installId").andIsOk

    enabledProjectIds().assert.isEmpty()
  }

  @Test
  fun `drops a project when the native install's availability is revoked`() {
    val native = registerNativeInstallGrantedToOrganization()
    userAccount = testData.user
    performAuthPut("/v2/projects/${testData.project.id}/apps/${native.first}", null).andIsOk
    val nativeToken = requestInstallToken(native.second, native.third)

    asToken(nativeToken, get(SELF_INSTALLATIONS)).andIsOk.andAssertThatJson {
      node("_embedded.installations[0].native").isEqualTo(true)
      node("_embedded.installations[0].enabledProjects").isArray.hasSize(1)
    }

    userAccount = testData.admin
    performAuthDelete(
      "/v2/administration/apps/${native.first}/organizations/${testData.organization.id}",
    ).andIsOk

    asToken(nativeToken, get(SELF_INSTALLATIONS)).andIsOk.andAssertThatJson {
      node("_embedded.installations[0].enabledProjects").isArray.isEmpty()
    }
  }

  /**
   * A user-context token acts for one signed-in user, who need not be a member of every project the
   * install is enabled for — and the iframe is handed its project in the init payload anyway.
   */
  @Test
  fun `refuses a user-context token`() {
    val userToken =
      appTokenService.mintUserContextToken(
        installId = installId,
        userId = testData.user.id,
        projectId = testData.project.id,
        isReadOnly = false,
      )

    asToken(userToken, get(SELF_INSTALLATIONS))
      .andIsForbidden
      .andHasErrorMessage(Message.APP_ACCESS_FORBIDDEN)
  }

  @Test
  fun `refuses a signed-in user session`() {
    performAuthGet(SELF_INSTALLATIONS)
      .andIsForbidden
      .andHasErrorMessage(Message.APP_ACCESS_FORBIDDEN)
  }

  @Test
  fun `refuses a project API key`() {
    val apiKey =
      apiKeyService.create(testData.user, setOf(Scope.ACTIVITY_VIEW), testData.project)

    logout()
    perform(get(SELF_INSTALLATIONS).header(API_KEY_HEADER_NAME, "tgpak_${apiKey.encodedKey}"))
      .andIsForbidden
      .andHasErrorMessage(Message.API_ACCESS_FORBIDDEN)
  }

  private fun enabledProjectIds(): List<Long> {
    val response =
      asToken(installToken, get(SELF_INSTALLATIONS))
        .andIsOk
        .andReturn()
        .response.contentAsString
    val projects = objectMapper.readTree(response).at("/_embedded/installations/0/enabledProjects")
    return projects.toList().map { it.get("id").asLong() }
  }

  /** @return the install id and its one-time client id / client secret. */
  private fun registerOrganizationInstall(): Triple<Long, String, String> {
    val response =
      performAuthPost(
        "/v2/organizations/${testData.organization.id}/apps",
        mapOf("manifestUrl" to AppsTestFixtures.MANIFEST_URL),
      ).andIsOk.andReturn().response.contentAsString
    return credentialsOf(response)
  }

  private fun registerNativeInstallGrantedToOrganization(): Triple<Long, String, String> {
    userAccount = testData.admin
    AppsTestFixtures.mockManifest(appManifestHttpClient, NATIVE_MANIFEST)
    val response =
      performAuthPost(
        "/v2/administration/apps",
        mapOf("manifestUrl" to AppsTestFixtures.MANIFEST_URL),
      ).andIsOk.andReturn().response.contentAsString
    val credentials = credentialsOf(response)
    performAuthPut(
      "/v2/administration/apps/${credentials.first}/organizations/${testData.organization.id}",
      null,
    ).andIsOk
    return credentials
  }

  private fun credentialsOf(registrationResponse: String): Triple<Long, String, String> {
    val json = objectMapper.readTree(registrationResponse)
    return Triple(
      json.get("id").asLong(),
      json.get("clientId").asText(),
      json.get("clientSecret").asText(),
    )
  }

  private fun asToken(
    token: String,
    builder: MockHttpServletRequestBuilder,
  ): ResultActions {
    logout()
    return perform(builder.header(HttpHeaders.AUTHORIZATION, "Bearer $token"))
  }

  private fun requestInstallToken(
    clientId: String,
    clientSecret: String,
  ): String {
    val response =
      perform(
        post("/v2/public/apps/token")
          .contentType(MediaType.APPLICATION_JSON)
          .content(
            objectMapper.writeValueAsString(
              mapOf(
                "grant_type" to "client_credentials",
                "client_id" to clientId,
                "client_secret" to clientSecret,
              ),
            ),
          ),
      ).andIsOk.andReturn().response.contentAsString
    return objectMapper.readTree(response).get("access_token").asText()
  }

  companion object {
    private const val SELF_INSTALLATIONS = "/v2/apps/self/installations"

    private val MANIFEST: String =
      """
      {
        "id": "test-app",
        "name": "Test App",
        "version": "0.1.0",
        "baseUrl": "https://app.example.com",
        "scopes": ["activity.view"],
        "modules": {
          "project-dashboard-page": [
            {"key": "home", "title": "Home", "icon": "🏠", "entry": "/"}
          ]
        }
      }
      """.trimIndent()

    private val NATIVE_MANIFEST: String =
      """
      {
        "id": "native-test-app",
        "name": "Native Test App",
        "version": "0.1.0",
        "baseUrl": "https://native.example.com",
        "scopes": ["activity.view"],
        "modules": {
          "project-dashboard-page": [
            {"key": "home", "title": "Home", "icon": "🏠", "entry": "/"}
          ]
        }
      }
      """.trimIndent()
  }
}
