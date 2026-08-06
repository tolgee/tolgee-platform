package io.tolgee.api.v2.controllers.apps

import io.tolgee.development.testDataBuilder.data.AppsTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.node
import io.tolgee.service.apps.AppInstallService
import io.tolgee.service.apps.AppManifestHttpClient
import io.tolgee.service.apps.AppsTestFixtures
import io.tolgee.service.apps.lifecycle.AppLifecycleHttpClient
import io.tolgee.testing.AuthorizedControllerTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class AppTokenEndpointControllerTest : AuthorizedControllerTest() {
  @Autowired
  lateinit var appInstallService: AppInstallService

  @MockitoBean
  @Autowired
  lateinit var appManifestHttpClient: AppManifestHttpClient

  @MockitoBean
  @Autowired
  lateinit var appLifecycleHttpClient: AppLifecycleHttpClient

  lateinit var testData: AppsTestData
  lateinit var clientId: String
  lateinit var clientSecret: String

  @BeforeEach
  fun setup() {
    testData = AppsTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    AppsTestFixtures.mockManifest(appManifestHttpClient, MANIFEST_WITH_SCOPES)
    val response =
      performAuthPost(
        "/v2/organizations/${testData.organization.id}/apps/register",
        mapOf("manifestUrl" to AppsTestFixtures.MANIFEST_URL),
      ).andIsOk.andReturn().response.contentAsString
    val json = objectMapper.readTree(response)
    clientId = json.get("clientId").asText()
    clientSecret = json.get("clientSecret").asText()
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `exchanges valid client credentials for an install-context access token`() {
    tokenRequest(clientId, clientSecret, "client_credentials").andIsOk.andAssertThatJson {
      node("access_token").isString.isNotEmpty()
      node("token_type").isEqualTo("Bearer")
      node("expires_in").isNumber
    }
  }

  @Test
  fun `rejects a wrong client secret with 401`() {
    tokenRequest(clientId, "tgapps_wrong-secret", "client_credentials")
      .andExpect(status().isUnauthorized)
      .andAssertThatJson { node("code").isEqualTo("invalid_app_credentials") }
  }

  @Test
  fun `rejects an unknown client id with 401`() {
    tokenRequest("tgapp_does-not-exist", clientSecret, "client_credentials")
      .andExpect(status().isUnauthorized)
      .andAssertThatJson { node("code").isEqualTo("invalid_app_credentials") }
  }

  @Test
  fun `rejects an unsupported grant type with 400`() {
    tokenRequest(clientId, clientSecret, "authorization_code")
      .andExpect(status().isBadRequest)
      .andAssertThatJson { node("code").isEqualTo("app_unsupported_grant_type") }
  }

  @Test
  fun `the returned access token authenticates as the install for an enabled project`() {
    val installId = appInstallService.findAll(testData.organization.id).single().id
    performAuthPut("/v2/projects/${testData.project.id}/apps/$installId", null).andIsOk

    val response =
      tokenRequest(clientId, clientSecret, "client_credentials")
        .andIsOk
        .andReturn()
        .response.contentAsString
    val token =
      objectMapper
        .readTree(response)
        .get("access_token")
        .asText()

    perform(
      get("/v2/projects/${testData.project.id}/translations")
        .header(HttpHeaders.AUTHORIZATION, "Bearer $token"),
    ).andIsOk
  }

  private fun tokenRequest(
    clientId: String,
    clientSecret: String,
    grantType: String,
  ): ResultActions {
    logout()
    return perform(
      post("/v2/public/apps/token")
        .contentType(MediaType.APPLICATION_JSON)
        .content(
          objectMapper.writeValueAsString(
            mapOf(
              "grant_type" to grantType,
              "client_id" to clientId,
              "client_secret" to clientSecret,
            ),
          ),
        ),
    )
  }

  companion object {
    private val MANIFEST_WITH_SCOPES: String =
      """
      {
        "id": "test-app",
        "name": "Test App",
        "version": "0.1.0",
        "baseUrl": "https://app.example.com",
        "scopes": ["translations.view", "keys.edit"],
        "modules": {
          "project-dashboard-page": [
            {"key": "home", "title": "Home", "icon": "🏠", "entry": "/"}
          ]
        }
      }
      """.trimIndent()
  }
}
