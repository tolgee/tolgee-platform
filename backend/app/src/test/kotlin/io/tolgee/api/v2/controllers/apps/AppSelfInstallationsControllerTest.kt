package io.tolgee.api.v2.controllers.apps

import io.tolgee.API_KEY_HEADER_NAME
import io.tolgee.constants.Message
import io.tolgee.development.testDataBuilder.data.AppsTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andHasErrorMessage
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsOk
import io.tolgee.model.enums.Scope
import io.tolgee.security.authentication.AppTokenService
import io.tolgee.security.authentication.AuthenticationFilter
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
 * Discovery from `/v2/apps/self/installations`: an app-level token lists the app's installations, and
 * nothing but an app-level token reaches it.
 */
class AppSelfInstallationsControllerTest : AuthorizedControllerTest() {
  @Autowired
  lateinit var appTokenService: AppTokenService

  @MockitoBean
  @Autowired
  lateinit var appManifestHttpClient: AppManifestHttpClient

  lateinit var testData: AppsTestData
  var installId: Long = 0
  lateinit var appClientId: String
  lateinit var appClientSecret: String
  lateinit var appLevelToken: String

  @BeforeEach
  fun setup() {
    testData = AppsTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    AppsTestFixtures.mockManifest(appManifestHttpClient, MANIFEST)

    val registration = registerOrganizationInstall()
    installId = registration.first
    appClientId = registration.second
    appClientSecret = registration.third
    performAuthPut("/v2/projects/${testData.project.id}/apps/$installId", null).andIsOk
    appLevelToken = requestToken(appClientId, appClientSecret, installId = null)
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `lists the app's installations with the projects each is enabled for`() {
    asToken(appLevelToken, get(SELF_INSTALLATIONS)).andIsOk.andAssertThatJson {
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
  fun `reflects a project becoming enabled`() {
    enabledProjectIds().assert.containsExactly(testData.project.id)

    userAccount = testData.user
    performAuthPut("/v2/projects/${testData.siblingProject.id}/apps/$installId", null).andIsOk

    enabledProjectIds().assert.containsExactlyInAnyOrder(
      testData.project.id,
      testData.siblingProject.id,
    )
  }

  @Test
  fun `drops a project once the app is disabled for it`() {
    enabledProjectIds().assert.containsExactly(testData.project.id)

    userAccount = testData.user
    performAuthDelete("/v2/projects/${testData.project.id}/apps/$installId").andIsOk

    enabledProjectIds().assert.isEmpty()
  }

  @Test
  fun `refuses an install-context token`() {
    val installToken = requestToken(appClientId, appClientSecret, installId)

    asToken(installToken, get(SELF_INSTALLATIONS))
      .andIsForbidden
      .andHasErrorMessage(Message.APP_ACCESS_FORBIDDEN)
  }

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

  /** Acting-as is meaningless for an app-level token; the header is ignored, never resolved to a user. */
  @Test
  fun `ignores an acting-as header on the app-level route`() {
    logout()
    perform(
      get(SELF_INSTALLATIONS)
        .header(HttpHeaders.AUTHORIZATION, "Bearer $appLevelToken")
        .header(AuthenticationFilter.ACTING_AS_USER_HEADER, "9999999"),
    ).andIsOk
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
      asToken(appLevelToken, get(SELF_INSTALLATIONS))
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
        "/v2/organizations/${testData.organization.id}/owned-apps",
        mapOf("manifestUrl" to AppsTestFixtures.MANIFEST_URL),
      ).andIsOk.andReturn().response.contentAsString
    val json = objectMapper.readTree(response)
    return Triple(
      json.get("installId").asLong(),
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

  private fun requestToken(
    clientId: String,
    clientSecret: String,
    installId: Long?,
  ): String {
    logout()
    val body =
      mutableMapOf<String, Any>(
        "grant_type" to "client_credentials",
        "client_id" to clientId,
        "client_secret" to clientSecret,
      )
    installId?.let { body["install_id"] = it }
    val response =
      perform(
        post("/v2/public/apps/token")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(body)),
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
  }
}
