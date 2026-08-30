package io.tolgee.api.v2.controllers.apps

import io.tolgee.development.testDataBuilder.data.AppsTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsNotFound
import io.tolgee.fixtures.andIsOk
import io.tolgee.model.enums.Scope
import io.tolgee.service.apps.AppInstallService
import io.tolgee.service.apps.AppManifestHttpClient
import io.tolgee.service.apps.AppsTestFixtures
import io.tolgee.service.apps.lifecycle.AppLifecycleHttpClient
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post

/**
 * Manifest refresh and re-consent: the org owner's refresh approves the scopes a manifest now
 * requests, while an app refreshing its own install can never widen the grant.
 */
class AppRefreshTest : AuthorizedControllerTest() {
  @Autowired
  lateinit var appInstallService: AppInstallService

  @MockitoBean
  @Autowired
  lateinit var appManifestHttpClient: AppManifestHttpClient

  @MockitoBean
  @Autowired
  lateinit var appLifecycleHttpClient: AppLifecycleHttpClient

  lateinit var testData: AppsTestData
  var installId: Long = 0
  lateinit var appClientId: String
  lateinit var appClientSecret: String

  @BeforeEach
  fun setup() {
    testData = AppsTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    mockManifest(manifest(scopes = """"translations.view", "keys.edit""""))

    val response =
      performAuthPost(ownedUrl(), mapOf("manifestUrl" to AppsTestFixtures.MANIFEST_URL))
        .andIsOk
        .andReturn()
        .response.contentAsString
    val json = objectMapper.readTree(response)
    installId = json.get("installId").asLong()
    appClientId = json.get("clientId").asText()
    appClientSecret = json.get("clientSecret").asText()
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `owner refresh re-fetches the manifest snapshot`() {
    mockManifest(manifest(name = "Renamed App", version = "0.2.0", scopes = """"translations.view", "keys.edit""""))

    performAuthPost("${appsUrl()}/$installId/refresh", null).andIsOk.andAssertThatJson {
      node("name").isEqualTo("Renamed App")
      node("version").isEqualTo("0.2.0")
    }

    performAuthGet(appsUrl()).andIsOk.andAssertThatJson {
      node("_embedded.appInstalls[0].name").isEqualTo("Renamed App")
      node("_embedded.appInstalls[0].version").isEqualTo("0.2.0")
    }
  }

  @Test
  fun `an app-initiated refresh surfaces a widened scope as pending without granting it`() {
    mockManifest(manifest(scopes = """"translations.view", "keys.edit", "keys.create""""))

    asAppToken(post("${selfInstallations()}/$installId/refresh")).andIsOk.andAssertThatJson {
      node("scopes").isArray.containsExactlyInAnyOrder("translations.view", "keys.edit")
      node("pendingScopes").isArray.containsExactly("keys.create")
    }

    grantedScopeValues().assert.containsExactlyInAnyOrder("translations.view", "keys.edit")

    performAuthGet(appsUrl()).andIsOk.andAssertThatJson {
      node("_embedded.appInstalls[0].scopes").isArray.containsExactlyInAnyOrder("translations.view", "keys.edit")
      node("_embedded.appInstalls[0].pendingScopes").isArray.containsExactly("keys.create")
    }
  }

  @Test
  fun `an owner refresh approves the widened scopes`() {
    mockManifest(manifest(scopes = """"translations.view", "keys.edit", "keys.create""""))
    asAppToken(post("${selfInstallations()}/$installId/refresh")).andIsOk

    performAuthPost("${appsUrl()}/$installId/refresh", null).andIsOk.andAssertThatJson {
      node("scopes").isArray.containsExactlyInAnyOrder("translations.view", "keys.edit", "keys.create")
      node("pendingScopes").isArray.isEmpty()
    }

    grantedScopeValues().assert.containsExactlyInAnyOrder("translations.view", "keys.edit", "keys.create")
  }

  @Test
  fun `an app-initiated refresh drops a scope the manifest no longer requests`() {
    mockManifest(manifest(scopes = """"translations.view""""))

    asAppToken(post("${selfInstallations()}/$installId/refresh")).andIsOk.andAssertThatJson {
      node("scopes").isArray.containsExactly("translations.view")
      node("pendingScopes").isArray.isEmpty()
    }

    grantedScopeValues().assert.containsExactly("translations.view")
  }

  @Test
  fun `refresh does not reach an install belonging to another organization`() {
    mockManifest(manifest(scopes = """"translations.view", "keys.edit", "keys.create""""))

    userAccount = testData.otherOwner
    performAuthPost("/v2/organizations/${testData.otherOrganization.id}/apps/$installId/refresh", null)
      .andIsNotFound

    grantedScopeValues().assert.containsExactlyInAnyOrder("translations.view", "keys.edit")
  }

  private fun grantedScopeValues(): List<String> =
    appInstallService.find(testData.organization.id, installId)!!.grantedScopes.map { it.value }

  private fun appsUrl() = "/v2/organizations/${testData.organization.id}/apps"

  private fun ownedUrl() = "/v2/organizations/${testData.organization.id}/owned-apps"

  private fun selfInstallations() = "/v2/apps/self/installations"

  private fun mockManifest(json: String) = AppsTestFixtures.mockManifest(appManifestHttpClient, json)

  private fun asAppToken(builder: MockHttpServletRequestBuilder): ResultActions {
    val token = appLevelToken()
    logout()
    val result = perform(builder.header(HttpHeaders.AUTHORIZATION, "Bearer $token"))
    userAccount = testData.user
    return result
  }

  private fun appLevelToken(): String {
    logout()
    val body =
      mapOf(
        "grant_type" to "client_credentials",
        "client_id" to appClientId,
        "client_secret" to appClientSecret,
      )
    val response =
      perform(
        post("/v2/public/apps/token")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(body)),
      ).andIsOk.andReturn().response.contentAsString
    userAccount = testData.user
    return objectMapper.readTree(response).get("access_token").asText()
  }

  private fun manifest(
    name: String = "Test App",
    version: String = "0.1.0",
    scopes: String,
  ): String =
    """
    {
      "id": "test-app",
      "name": "$name",
      "version": "$version",
      "baseUrl": "https://app.example.com",
      "scopes": [$scopes],
      "modules": {
        "project-dashboard-page": [
          {"key": "home", "title": "Home", "icon": "🏠", "entry": "/"}
        ]
      }
    }
    """.trimIndent()
}
