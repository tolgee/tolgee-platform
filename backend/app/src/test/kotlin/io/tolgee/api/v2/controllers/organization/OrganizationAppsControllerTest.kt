package io.tolgee.api.v2.controllers.organization

import io.tolgee.development.testDataBuilder.data.AppsTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.node
import io.tolgee.model.enums.Scope
import io.tolgee.service.apps.AppInstallService
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.anyString
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestTemplate

class OrganizationAppsControllerTest : AuthorizedControllerTest() {
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
  fun `registers an app from a valid manifest`() {
    mockManifest(validManifest())
    performAuthPost(
      appsUrl(),
      mapOf("manifestUrl" to "https://example.com/manifest.json"),
    ).andIsOk.andAssertThatJson {
      node("appId").isEqualTo("test-app")
      node("name").isEqualTo("Test App")
      node("version").isEqualTo("0.1.0")
      node("baseUrl").isEqualTo("https://app.example.com")
      node("modules.project-dashboard-page[0].key").isEqualTo("home")
      node("modules.project-dashboard-page[0].title").isEqualTo("Home")
      node("modules.project-dashboard-page[0].entry").isEqualTo("/")
      node("scopes").isArray.containsExactlyInAnyOrder("translations.view", "keys.edit")
    }
    val install = appInstallService.findAll(testData.organization.id).single()
    assertThat(install.grantedScopes).containsExactlyInAnyOrder(Scope.TRANSLATIONS_VIEW, Scope.KEYS_EDIT)
  }

  @Test
  fun `parses key-edit-tab and key+translation-action modules with decoratorsUrl`() {
    mockManifest(manifestWithActions())
    performAuthPost(appsUrl(), registerBody()).andIsOk.andAssertThatJson {
      node("decoratorsUrl").isEqualTo("https://app.example.com/decorators")
      node("modules.key-edit-tab[0].key").isEqualTo("audit")
      node("modules.key-edit-tab[0].title").isEqualTo("Audit")
      node("modules.key-edit-tab[0].entry").isEqualTo("/key-edit-tab/audit")
      node("modules.key-action[0].key").isEqualTo("view-source")
      node("modules.key-action[0].type").isEqualTo("link")
      node("modules.key-action[0].urlTemplate").isEqualTo("https://example.com/{keyName}")
      node("modules.key-action[1].key").isEqualTo("open-audit")
      node("modules.key-action[1].type").isEqualTo("tab")
      node("modules.key-action[1].dynamic").isEqualTo(true)
      node("modules.key-action[1].tabKey").isEqualTo("audit")
      node("modules.translation-action[0].key").isEqualTo("show-activity")
      node("modules.translation-action[0].type").isEqualTo("panel")
      node("modules.translation-action[0].dynamic").isEqualTo(true)
      node("modules.translation-action[0].panelKey").isEqualTo("activity")
    }
  }

  @Test
  fun `rejects key-action of type tab whose tabKey doesn't resolve`() {
    mockManifest(manifestWithActions().replace("\"tabKey\": \"audit\"", "\"tabKey\": \"nope\""))
    performAuthPost(appsUrl(), registerBody()).andIsBadRequest.andAssertThatJson {
      node("code").isEqualTo("app_manifest_invalid")
    }
  }

  @Test
  fun `rejects translation-action of type panel whose panelKey doesn't resolve`() {
    mockManifest(manifestWithActions().replace("\"panelKey\": \"activity\"", "\"panelKey\": \"nope\""))
    performAuthPost(appsUrl(), registerBody()).andIsBadRequest.andAssertThatJson {
      node("code").isEqualTo("app_manifest_invalid")
    }
  }

  @Test
  fun `parses translation-tools-panel modules alongside dashboard pages`() {
    mockManifest(manifestWithToolsPanel())
    performAuthPost(appsUrl(), registerBody()).andIsOk.andAssertThatJson {
      node("modules.project-dashboard-page[0].key").isEqualTo("home")
      node("modules.translation-tools-panel[0].key").isEqualTo("activity")
      node("modules.translation-tools-panel[0].title").isEqualTo("Activity")
      node("modules.translation-tools-panel[0].icon").isEqualTo("📈")
      node("modules.translation-tools-panel[0].entry").isEqualTo("/tools-panel")
    }
  }

  @Test
  fun `preview returns parsed manifest with requested scopes without persisting`() {
    mockManifest(validManifest())
    performAuthPost("${appsUrl()}/preview", registerBody()).andIsOk.andAssertThatJson {
      node("appId").isEqualTo("test-app")
      node("name").isEqualTo("Test App")
      node("version").isEqualTo("0.1.0")
      node("requestedScopes").isArray.containsExactlyInAnyOrder("translations.view", "keys.edit")
    }
    assertThat(appInstallService.findAll(testData.organization.id)).isEmpty()
  }

  @Test
  fun `refresh updates granted scopes from new manifest`() {
    mockManifest(validManifest())
    performAuthPost(appsUrl(), registerBody()).andIsOk
    val installId = appInstallService.findAll(testData.organization.id).single().id

    mockManifest(validManifestV2WithExtraScope())
    performAuthPost("${appsUrl()}/$installId/refresh", emptyMap<String, Any>()).andIsOk.andAssertThatJson {
      node("scopes")
        .isArray
        .containsExactlyInAnyOrder("translations.view", "keys.edit", "screenshots.upload")
    }
    assertThat(appInstallService.find(installId)!!.grantedScopes)
      .containsExactlyInAnyOrder(Scope.TRANSLATIONS_VIEW, Scope.KEYS_EDIT, Scope.SCREENSHOTS_UPLOAD)
  }

  @Test
  fun `rejects manifest with an unknown scope`() {
    mockManifest(validManifest().replace("\"keys.edit\"", "\"not.a.real.scope\""))
    performAuthPost(appsUrl(), registerBody()).andIsBadRequest.andAssertThatJson {
      node("code").isEqualTo("app_manifest_invalid")
    }
  }

  @Test
  fun `register without scopes block stores no granted scopes`() {
    mockManifest(manifestWithoutScopes())
    performAuthPost(appsUrl(), registerBody()).andIsOk.andAssertThatJson {
      node("scopes").isArray.isEmpty()
    }
    val install = appInstallService.findAll(testData.organization.id).single()
    assertThat(install.grantedScopes).isEmpty()
  }

  @Test
  fun `rejects duplicate app for the same organization`() {
    mockManifest(validManifest())
    performAuthPost(appsUrl(), registerBody()).andIsOk

    performAuthPost(appsUrl(), registerBody()).andIsBadRequest.andAssertThatJson {
      node("code").isEqualTo("app_already_installed")
    }
  }

  @Test
  fun `rejects invalid manifest JSON`() {
    mockManifest("not valid json")
    performAuthPost(appsUrl(), registerBody()).andIsBadRequest.andAssertThatJson {
      node("code").isEqualTo("app_manifest_invalid")
    }
  }

  @Test
  fun `rejects unreachable manifest URL`() {
    doThrow(ResourceAccessException("connection refused"))
      .whenever(restTemplate)
      .getForObject(anyString(), eq(String::class.java))

    performAuthPost(appsUrl(), registerBody()).andIsBadRequest.andAssertThatJson {
      node("code").isEqualTo("app_manifest_fetch_failed")
    }
  }

  @Test
  fun `lists registered apps for the organization`() {
    mockManifest(validManifest())
    performAuthPost(appsUrl(), registerBody()).andIsOk

    performAuthGet(appsUrl()).andIsOk.andAssertThatJson {
      node("_embedded.appInstalls").isArray.hasSize(1)
      node("_embedded.appInstalls[0].appId").isEqualTo("test-app")
    }
  }

  @Test
  fun `refresh updates the stored manifest`() {
    mockManifest(validManifest())
    performAuthPost(appsUrl(), registerBody()).andIsOk
    val installId = appInstallService.findAll(testData.organization.id).single().id

    mockManifest(validManifestV2())
    performAuthPost("${appsUrl()}/$installId/refresh", emptyMap<String, Any>()).andIsOk.andAssertThatJson {
      node("version").isEqualTo("0.2.0")
      node("modules.project-dashboard-page[0].title").isEqualTo("Home v2")
    }
  }

  @Test
  fun `refresh rejects manifest whose app id changed`() {
    mockManifest(validManifest())
    performAuthPost(appsUrl(), registerBody()).andIsOk
    val installId = appInstallService.findAll(testData.organization.id).single().id

    mockManifest(validManifest().replace("\"test-app\"", "\"different-app\""))
    performAuthPost("${appsUrl()}/$installId/refresh", emptyMap<String, Any>()).andIsBadRequest.andAssertThatJson {
      node("code").isEqualTo("app_manifest_invalid")
    }
  }

  @Test
  fun `removes a registered app`() {
    mockManifest(validManifest())
    performAuthPost(appsUrl(), registerBody()).andIsOk
    val installId = appInstallService.findAll(testData.organization.id).single().id

    performAuthDelete("${appsUrl()}/$installId").andIsOk
    assertThat(appInstallService.findAll(testData.organization.id)).isEmpty()
  }

  private fun appsUrl() = "/v2/organizations/${testData.organization.id}/apps"

  private fun registerBody() = mapOf("manifestUrl" to "https://example.com/manifest.json")

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

  private fun validManifestV2WithExtraScope(): String =
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

  private fun manifestWithToolsPanel(): String =
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
        ],
        "translation-tools-panel": [
          {"key": "activity", "title": "Activity", "icon": "📈", "entry": "/tools-panel"}
        ]
      }
    }
    """.trimIndent()

  private fun manifestWithActions(): String =
    """
    {
      "id": "test-app",
      "name": "Test App",
      "version": "0.1.0",
      "baseUrl": "https://app.example.com",
      "decoratorsUrl": "https://app.example.com/decorators",
      "scopes": ["translations.view", "keys.edit"],
      "modules": {
        "project-dashboard-page": [
          {"key": "home", "title": "Home", "icon": "🏠", "entry": "/"}
        ],
        "translation-tools-panel": [
          {"key": "activity", "title": "Activity", "icon": "📈", "entry": "/tools-panel"}
        ],
        "key-edit-tab": [
          {"key": "audit", "title": "Audit", "icon": "🛡️", "entry": "/key-edit-tab/audit"}
        ],
        "key-action": [
          {
            "key": "view-source",
            "type": "link",
            "icon": "🔗",
            "tooltip": "View source",
            "urlTemplate": "https://example.com/{keyName}"
          },
          {
            "key": "open-audit",
            "type": "tab",
            "icon": "🛡️",
            "tooltip": "Audit info",
            "dynamic": true,
            "tabKey": "audit"
          }
        ],
        "translation-action": [
          {
            "key": "show-activity",
            "type": "panel",
            "icon": "📈",
            "tooltip": "Activity",
            "dynamic": true,
            "panelKey": "activity"
          }
        ]
      }
    }
    """.trimIndent()

  private fun manifestWithoutScopes(): String =
    """
    {
      "id": "test-app",
      "name": "Test App",
      "version": "0.1.0",
      "baseUrl": "https://app.example.com",
      "modules": {
        "project-dashboard-page": [
          {"key": "home", "title": "Home", "icon": "🏠", "entry": "/"}
        ]
      }
    }
    """.trimIndent()
}
