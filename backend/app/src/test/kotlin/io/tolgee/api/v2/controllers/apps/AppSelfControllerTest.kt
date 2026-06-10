package io.tolgee.api.v2.controllers.apps

import io.tolgee.development.testDataBuilder.data.AppsTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.andIsUnauthorized
import io.tolgee.fixtures.node
import io.tolgee.model.enums.Scope
import io.tolgee.service.apps.AppInstallService
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.anyString
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.web.client.RestTemplate

class AppSelfControllerTest : AuthorizedControllerTest() {
  @Autowired
  lateinit var appInstallService: AppInstallService

  @MockitoBean
  @Autowired
  lateinit var restTemplate: RestTemplate

  lateinit var testData: AppsTestData

  @BeforeEach
  fun setup() {
    testData = AppsTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
  }

  @Test
  fun `updates own manifest URL when authenticated with clientSecret`() {
    mockManifest(validManifest())
    val clientSecret = registerAndGetClientSecret()

    mockManifest(validManifestV2())
    performSelfPatch(
      clientSecret,
      mapOf("manifestUrl" to "https://example.com/v2/manifest.json"),
    ).andIsOk.andAssertThatJson {
      node("version").isEqualTo("0.2.0")
      node("modules.project-dashboard-page[0].title").isEqualTo("Home v2")
    }

    val install = appInstallService.findAll(testData.organization.id).single()
    assertThat(install.manifestUrl).isEqualTo("https://example.com/v2/manifest.json")
  }

  @Test
  fun `rejects manifest whose app id changed`() {
    mockManifest(validManifest())
    val clientSecret = registerAndGetClientSecret()

    mockManifest(validManifest().replace("\"test-app\"", "\"different-app\""))
    performSelfPatch(
      clientSecret,
      mapOf("manifestUrl" to "https://example.com/other/manifest.json"),
    ).andIsBadRequest.andAssertThatJson {
      node("code").isEqualTo("app_manifest_invalid")
    }
  }

  @Test
  fun `does not widen granted scopes on self manifest update`() {
    mockManifest(validManifest())
    val clientSecret = registerAndGetClientSecret()

    // A plugin must not be able to self-grant a scope the owner never consented to by repointing
    // at a manifest that declares more scopes.
    mockManifest(validManifestWithExtraScope())
    performSelfPatch(
      clientSecret,
      mapOf("manifestUrl" to "https://example.com/v2/manifest.json"),
    ).andIsOk.andAssertThatJson {
      node("scopes").isArray.containsExactlyInAnyOrder("translations.view", "keys.edit")
    }

    val install = appInstallService.findAll(testData.organization.id).single()
    assertThat(install.grantedScopes).containsExactlyInAnyOrder(Scope.TRANSLATIONS_VIEW, Scope.KEYS_EDIT)
  }

  @Test
  fun `rejects requests with wrong clientSecret`() {
    mockManifest(validManifest())
    registerAndGetClientSecret()

    performSelfPatch(
      "tgapps_notarealsecretjustbogusbytesforauthnegativetesting",
      mapOf("manifestUrl" to "https://example.com/v2/manifest.json"),
    ).andIsUnauthorized
  }

  @Test
  fun `rejects requests without any credentials`() {
    perform(
      patch("/v2/apps/self/manifest-url")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(mapOf("manifestUrl" to "https://x/m.json"))),
    ).andIsForbidden
  }

  private fun registerAndGetClientSecret(): String {
    val response =
      performAuthPost(
        "/v2/organizations/${testData.organization.id}/apps",
        mapOf("manifestUrl" to "https://example.com/manifest.json"),
      ).andIsOk.andReturn().response.contentAsString
    val tree = objectMapper.readTree(response)
    return tree.get("clientSecret").asText()
  }

  private fun performSelfPatch(
    clientSecret: String,
    body: Any,
  ): ResultActions {
    return perform(
      patch("/v2/apps/self/manifest-url")
        .header("X-API-Key", clientSecret)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(body)),
    )
  }

  private fun mockManifest(json: String) {
    doReturn(json).whenever(restTemplate).getForObject(anyString(), eq(String::class.java))
  }

  private fun validManifest(): String =
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

  private fun validManifestV2(): String =
    """
    {
      "id": "test-app",
      "name": "Test App",
      "version": "0.2.0",
      "baseUrl": "https://app.example.com",
      "scopes": ["translations.view", "keys.edit"],
      "modules": {
        "project-dashboard-page": [
          {"key": "home", "title": "Home v2", "icon": "🏠", "entry": "/"}
        ]
      }
    }
    """.trimIndent()

  private fun validManifestWithExtraScope(): String =
    """
    {
      "id": "test-app",
      "name": "Test App",
      "version": "0.2.0",
      "baseUrl": "https://app.example.com",
      "scopes": ["translations.view", "keys.edit", "screenshots.upload"],
      "modules": {
        "project-dashboard-page": [
          {"key": "home", "title": "Home v2", "icon": "🏠", "entry": "/"}
        ]
      }
    }
    """.trimIndent()
}
