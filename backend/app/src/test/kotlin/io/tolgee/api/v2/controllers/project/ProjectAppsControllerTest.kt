package io.tolgee.api.v2.controllers.project

import io.tolgee.development.testDataBuilder.data.AppsTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsNotFound
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.node
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
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get

class ProjectAppsControllerTest : AuthorizedControllerTest() {
  @Autowired
  lateinit var appInstallService: AppInstallService

  @MockitoBean
  @Autowired
  lateinit var appManifestHttpClient: AppManifestHttpClient

  @MockitoBean
  @Autowired
  lateinit var appLifecycleHttpClient: AppLifecycleHttpClient

  lateinit var testData: AppsTestData

  @BeforeEach
  fun setup() {
    testData = AppsTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    registerApp()
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `public configuration reports apps as enabled`() {
    performAuthGet("/api/public/configuration").andIsOk.andAssertThatJson {
      node("appsEnabled").isEqualTo(true)
    }
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
  fun `the listing rejects a project member without project edit permission`() {
    userAccount = testData.member
    performAuthGet(projectAppsUrl()).andIsForbidden
  }

  @Test
  fun `the listing does not expose the manifest url, which is owner-only at org level`() {
    performAuthGet(projectAppsUrl()).andIsOk.andAssertThatJson {
      node("_embedded.projectApps[0].manifestUrl").isAbsent()
    }
  }

  @Test
  fun `exposes dashboard page modules to the project listing`() {
    performAuthGet(projectAppsUrl()).andIsOk.andAssertThatJson {
      node("_embedded.projectApps[0].modules.project-dashboard-page[0].key").isEqualTo("home")
      node("_embedded.projectApps[0].modules.project-dashboard-page[0].title").isEqualTo("Home")
      node("_embedded.projectApps[0].modules.project-dashboard-page[0].entry").isEqualTo("/")
    }
  }

  @Test
  fun `the enabled listing is readable by a project member without project edit permission`() {
    val installId = installId()
    performAuthPut("${projectAppsUrl()}/$installId", null).andIsOk

    userAccount = testData.member
    performAuthGet("${projectAppsUrl()}/enabled").andIsOk.andAssertThatJson {
      node("_embedded.projectApps").isArray.hasSize(1)
      node("_embedded.projectApps[0].appId").isEqualTo("test-app")
      node("_embedded.projectApps[0].enabled").isEqualTo(true)
    }
  }

  @Test
  fun `the enabled listing omits an app that is registered but not enabled`() {
    userAccount = testData.member
    performAuthGet("${projectAppsUrl()}/enabled").andIsOk.andAssertThatJson {
      node("_embedded.projectApps").isAbsent()
    }
  }

  @Test
  fun `the enabled listing hides the project from a user who is not a member`() {
    userAccount = testData.otherOwner
    performAuthGet("${projectAppsUrl()}/enabled").andIsNotFound
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
  fun `enable and disable reject a project member without project edit permission`() {
    val installId = installId()
    userAccount = testData.member

    performAuthPut("${projectAppsUrl()}/$installId", null).andIsForbidden
    performAuthDelete("${projectAppsUrl()}/$installId").andIsForbidden
  }

  @Test
  fun `enable does not reach an install belonging to another organization`() {
    val installId = installId()
    userAccount = testData.otherOwner

    performAuthPut("/v2/projects/${testData.otherProject.id}/apps/$installId", null).andIsNotFound
  }

  @Test
  fun `enabling an app for one project leaves a sibling project of the same org disabled`() {
    val installId = installId()
    performAuthPut("${projectAppsUrl()}/$installId", null).andIsOk

    performAuthGet("/v2/projects/${testData.siblingProject.id}/apps").andIsOk.andAssertThatJson {
      node("_embedded.projectApps[0].appId").isEqualTo("test-app")
      node("_embedded.projectApps[0].enabled").isEqualTo(false)
    }
  }

  @Test
  fun `removing the org-level install also clears its project enablements`() {
    val installId = installId()
    performAuthPut("${projectAppsUrl()}/$installId", null).andIsOk

    performAuthDelete("/v2/organizations/${testData.organization.id}/apps/$installId").andIsOk

    appInstallService.findAll(testData.organization.id).assert.isEmpty()
    performAuthGet(projectAppsUrl()).andIsOk.andAssertThatJson {
      node("_embedded.projectApps").isAbsent()
    }
  }

  @Test
  fun `mints a user-context token when the app is enabled for the project`() {
    val installId = installId()
    performAuthPut("${projectAppsUrl()}/$installId", null).andIsOk

    performAuthPost("${projectAppsUrl()}/$installId/token", null).andIsOk.andAssertThatJson {
      node("token").isString.isNotEmpty()
    }
  }

  @Test
  fun `refuses to mint a token when the app is not enabled for the project`() {
    val installId = installId()
    performAuthPost("${projectAppsUrl()}/$installId/token", null).andIsNotFound.andAssertThatJson {
      node("code").isEqualTo("app_install_not_found")
    }
  }

  @Test
  fun `the minted token authenticates as the user, capped to the install's scopes`() {
    val installId = installId()
    performAuthPut("${projectAppsUrl()}/$installId", null).andIsOk
    val token = mintToken(installId)

    // translations.view is granted to the install and held by the owner, so the token can read.
    perform(
      get("/v2/projects/${testData.project.id}/translations")
        .header(HttpHeaders.AUTHORIZATION, "Bearer $token"),
    ).andIsOk

    // project.edit is not among the install's scopes, so the token is capped out of it even though
    // the owner has it.
    perform(
      get(projectAppsUrl()).header(HttpHeaders.AUTHORIZATION, "Bearer $token"),
    ).andIsForbidden
  }

  private fun mintToken(installId: Long): String {
    val response =
      performAuthPost("${projectAppsUrl()}/$installId/token", null)
        .andIsOk
        .andReturn()
        .response.contentAsString
    return objectMapper.readTree(response).get("token").asText()
  }

  private fun projectAppsUrl() = "/v2/projects/${testData.project.id}/apps"

  private fun installId() = appInstallService.findAll(testData.organization.id).single().id

  private fun registerApp() {
    mockManifest(validManifest())
    performAuthPost(
      "/v2/organizations/${testData.organization.id}/apps/register",
      mapOf("manifestUrl" to AppsTestFixtures.MANIFEST_URL),
    ).andIsOk
  }

  private fun mockManifest(json: String) {
    AppsTestFixtures.mockManifest(appManifestHttpClient, json)
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
}
