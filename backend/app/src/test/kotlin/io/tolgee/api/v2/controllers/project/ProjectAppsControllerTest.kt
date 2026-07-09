package io.tolgee.api.v2.controllers.project

import io.tolgee.development.testDataBuilder.data.AppsTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsNotFound
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.node
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
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.web.client.RestTemplate

class ProjectAppsControllerTest : AuthorizedControllerTest() {
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
    registerApp()
  }

  @Test
  fun `lists apps with enablement initially false`() {
    performAuthGet(projectAppsUrl()).andIsOk.andAssertThatJson {
      node("_embedded.projectApps").isArray.hasSize(1)
      node("_embedded.projectApps[0].appId").isEqualTo("test-app")
      node("_embedded.projectApps[0].enabled").isEqualTo(false)
    }
  }

  @Test
  fun `exposes translation-tools-panel modules to the project listing`() {
    performAuthGet(projectAppsUrl()).andIsOk.andAssertThatJson {
      node("_embedded.projectApps[0].modules.project-dashboard-page[0].key").isEqualTo("home")
      node("_embedded.projectApps[0].modules.translation-tools-panel[0].key").isEqualTo("activity")
      node("_embedded.projectApps[0].modules.translation-tools-panel[0].title").isEqualTo("Activity")
      node("_embedded.projectApps[0].modules.translation-tools-panel[0].entry").isEqualTo("/tools-panel")
    }
  }

  @Test
  fun `enable flips the state to true`() {
    val installId = installId()
    performAuthPut("${projectAppsUrl()}/$installId", null).andIsOk.andAssertThatJson {
      node("appId").isEqualTo("test-app")
      node("enabled").isEqualTo(true)
    }
    performAuthGet(projectAppsUrl()).andIsOk.andAssertThatJson {
      node("_embedded.projectApps[0].enabled").isEqualTo(true)
    }
  }

  @Test
  fun `enable is idempotent`() {
    val installId = installId()
    performAuthPut("${projectAppsUrl()}/$installId", null).andIsOk
    performAuthPut("${projectAppsUrl()}/$installId", null).andIsOk
    performAuthGet(projectAppsUrl()).andIsOk.andAssertThatJson {
      node("_embedded.projectApps").isArray.hasSize(1)
      node("_embedded.projectApps[0].enabled").isEqualTo(true)
    }
  }

  @Test
  fun `enable rejects unknown install id`() {
    performAuthPut("${projectAppsUrl()}/999999", null).andIsNotFound.andAssertThatJson {
      node("code").isEqualTo("app_install_not_found")
    }
  }

  @Test
  fun `disable removes enablement`() {
    val installId = installId()
    performAuthPut("${projectAppsUrl()}/$installId", null).andIsOk
    performAuthDelete("${projectAppsUrl()}/$installId").andIsOk
    performAuthGet(projectAppsUrl()).andIsOk.andAssertThatJson {
      node("_embedded.projectApps[0].enabled").isEqualTo(false)
    }
  }

  @Test
  fun `disable is idempotent when not enabled`() {
    val installId = installId()
    performAuthDelete("${projectAppsUrl()}/$installId").andIsOk
    performAuthGet(projectAppsUrl()).andIsOk.andAssertThatJson {
      node("_embedded.projectApps[0].enabled").isEqualTo(false)
    }
  }

  @Test
  fun `removing the org-level install also clears its project enablements`() {
    val installId = installId()
    performAuthPut("${projectAppsUrl()}/$installId", null).andIsOk

    performAuthDelete("/v2/organizations/${testData.organization.id}/apps/$installId").andIsOk

    assertThat(appInstallService.findAll(testData.organization.id)).isEmpty()
    performAuthGet(projectAppsUrl()).andIsOk.andAssertThatJson {
      node("_embedded.projectApps").isAbsent()
    }
  }

  private fun projectAppsUrl() = "/v2/projects/${testData.project.id}/apps"

  private fun installId() = appInstallService.findAll(testData.organization.id).single().id

  private fun registerApp() {
    mockManifest(validManifest())
    performAuthPost(
      "/v2/organizations/${testData.organization.id}/apps",
      mapOf("manifestUrl" to "https://example.com/manifest.json"),
    ).andIsOk
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
}
