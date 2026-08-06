package io.tolgee.api.v2.controllers.organization

import io.tolgee.constants.Message
import io.tolgee.development.testDataBuilder.data.AppsTestData
import io.tolgee.exceptions.BadRequestException
import io.tolgee.fixtures.AuthorizedRequestFactory
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsNotFound
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.node
import io.tolgee.model.enums.Scope
import io.tolgee.service.apps.AppInstallService
import io.tolgee.service.apps.AppManifestHttpClient
import io.tolgee.service.apps.AppsTestFixtures
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.anyString
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.transaction.support.TransactionSynchronizationManager

class OrganizationAppsControllerTest : AuthorizedControllerTest() {
  @Autowired
  lateinit var appInstallService: AppInstallService

  @MockitoBean
  @Autowired
  lateinit var appManifestHttpClient: AppManifestHttpClient

  lateinit var testData: AppsTestData

  @BeforeEach
  fun setup() {
    testData = AppsTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `registers an app from a valid manifest`() {
    mockManifest(validManifest())
    performAuthPost(appsUrl(), registerBody()).andIsOk.andAssertThatJson {
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
    install.grantedScopes.assert.containsExactlyInAnyOrder(Scope.TRANSLATIONS_VIEW, Scope.KEYS_EDIT)
  }

  @Test
  fun `registration issues OAuth client credentials, exposing the secret exactly once`() {
    mockManifest(validManifest())
    performAuthPost(appsUrl(), registerBody()).andIsOk.andAssertThatJson {
      node("clientId").isString.startsWith(AppInstallService.CLIENT_ID_PREFIX)
      node("clientSecret").isString.startsWith(AppInstallService.CLIENT_SECRET_PREFIX)
    }

    performAuthGet(appsUrl()).andIsOk.andAssertThatJson {
      node("_embedded.appInstalls[0].clientId").isString.startsWith(AppInstallService.CLIENT_ID_PREFIX)
      node("_embedded.appInstalls[0].clientSecret").isNull()
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
    appInstallService.findAll(testData.organization.id).assert.isEmpty()
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
    appInstallService
      .find(testData.organization.id, installId)!!
      .grantedScopes.assert
      .containsExactlyInAnyOrder(Scope.TRANSLATIONS_VIEW, Scope.KEYS_EDIT, Scope.SCREENSHOTS_UPLOAD)
  }

  @Test
  fun `refresh and remove do not reach an install belonging to another organization`() {
    mockManifest(validManifest())
    performAuthPost(appsUrl(), registerBody()).andIsOk
    val installId = appInstallService.findAll(testData.organization.id).single().id

    val otherOrgId = testData.otherOrganization.id
    userAccount = testData.otherOwner

    performAuthPost("/v2/organizations/$otherOrgId/apps/$installId/refresh", emptyMap<String, Any>())
      .andIsNotFound
    performAuthDelete("/v2/organizations/$otherOrgId/apps/$installId").andIsNotFound

    appInstallService.find(testData.organization.id, installId).assert.isNotNull
  }

  @Test
  fun `every org apps endpoint rejects a non-owner org member`() {
    mockManifest(validManifest())
    performAuthPost(appsUrl(), registerBody()).andIsOk
    val installId = appInstallService.findAll(testData.organization.id).single().id

    userAccount = testData.member

    performAuthGet(appsUrl()).andIsForbidden
    performAuthPost(appsUrl(), registerBody()).andIsForbidden
    performAuthPost("${appsUrl()}/preview", registerBody()).andIsForbidden
    performAuthPost("${appsUrl()}/$installId/refresh", emptyMap<String, Any>()).andIsForbidden
    performAuthPatch("${appsUrl()}/$installId/manifest-url", registerBody()).andIsForbidden
    performAuthDelete("${appsUrl()}/$installId").andIsForbidden
  }

  @Test
  fun `rejects a manifest URL pointing at a private address`() {
    performAuthPost(
      appsUrl(),
      mapOf("manifestUrl" to "http://127.0.0.1/manifest.json"),
    ).andIsBadRequest.andAssertThatJson {
      node("code").isEqualTo("url_not_valid")
    }
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
    install.grantedScopes.assert.isEmpty()
  }

  @Test
  fun `rejects a manifest declaring an unsupported top-level decoratorsUrl feature`() {
    mockManifest(
      validManifest().replace(
        "\"scopes\":",
        "\"decoratorsUrl\": \"https://app.example.com/d\",\n\"scopes\":",
      ),
    )
    performAuthPost(appsUrl(), registerBody()).andIsBadRequest.andAssertThatJson {
      node("code").isEqualTo("app_manifest_invalid")
      node("params[0]").isEqualTo("unsupported manifest features: decoratorsUrl")
    }
  }

  @Test
  fun `rejects a manifest declaring a non-dashboard module`() {
    mockManifest(manifestWithExtraModule())
    performAuthPost(appsUrl(), registerBody()).andIsBadRequest.andAssertThatJson {
      node("code").isEqualTo("app_manifest_invalid")
      node("params[0]").isEqualTo("unsupported manifest features: key-action")
    }
  }

  @Test
  fun `rejects a manifest declaring both an unsupported feature and module, sorted and joined`() {
    mockManifest(
      manifestWithExtraModule()
        .replace("\"scopes\":", "\"webhooks\": {\"url\": \"https://app.example.com/wh\"},\n\"scopes\":"),
    )
    performAuthPost(appsUrl(), registerBody()).andIsBadRequest.andAssertThatJson {
      node("code").isEqualTo("app_manifest_invalid")
      node("params[0]").isEqualTo("unsupported manifest features: key-action, webhooks")
    }
  }

  @Test
  fun `rejects a manifest with no dashboard page module`() {
    mockManifest(manifestWithoutDashboardPage())
    performAuthPost(appsUrl(), registerBody()).andIsBadRequest.andAssertThatJson {
      node("code").isEqualTo("app_manifest_invalid")
    }
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
    doThrow(BadRequestException(Message.APP_MANIFEST_FETCH_FAILED))
      .whenever(appManifestHttpClient)
      .fetchBody(anyString())

    performAuthPost(appsUrl(), registerBody()).andIsBadRequest.andAssertThatJson {
      node("code").isEqualTo("app_manifest_fetch_failed")
    }
  }

  @Test
  fun `rejects a blank manifest URL with a validation error`() {
    performAuthPost(appsUrl(), mapOf("manifestUrl" to "")).andIsBadRequest.andAssertThatJson {
      node("STANDARD_VALIDATION.manifestUrl").isNotNull()
    }
  }

  @Test
  fun `rejects a manifest whose fields exceed the stored column length`() {
    mockManifest(validManifest().replace("\"Test App\"", "\"${"x".repeat(256)}\""))
    performAuthPost(appsUrl(), registerBody()).andIsBadRequest.andAssertThatJson {
      node("code").isEqualTo("app_manifest_invalid")
    }
  }

  @Test
  fun `rejects a manifest with a blank required field`() {
    mockManifest(validManifest().replace("\"Test App\"", "\"  \""))
    performAuthPost(appsUrl(), registerBody()).andIsBadRequest.andAssertThatJson {
      node("code").isEqualTo("app_manifest_invalid")
    }
  }

  @Test
  fun `rejects a manifest whose module entry does not resolve to an http url`() {
    mockManifest(validManifest().replace("\"entry\": \"/\"", "\"entry\": \"javascript:alert(1)\""))
    performAuthPost(appsUrl(), registerBody()).andIsBadRequest.andAssertThatJson {
      node("code").isEqualTo("app_manifest_invalid")
    }
  }

  @Test
  fun `manifest url update does not reach an install of another organization`() {
    mockManifest(validManifest())
    performAuthPost(appsUrl(), registerBody()).andIsOk
    val installId = appInstallService.findAll(testData.organization.id).single().id

    userAccount = testData.otherOwner
    performAuthPatch(
      "/v2/organizations/${testData.otherOrganization.id}/apps/$installId/manifest-url",
      mapOf("manifestUrl" to "https://example.com/other/manifest.json"),
    ).andIsNotFound
  }

  /** Pins the no-transaction invariant documented on [io.tolgee.service.apps.AppInstallService]. */
  @Test
  fun `does not hold a transaction open while fetching the manifest`() {
    val transactionActiveDuringFetch = mutableListOf<Boolean>()
    doAnswer {
      transactionActiveDuringFetch.add(TransactionSynchronizationManager.isActualTransactionActive())
      validManifest()
    }.whenever(appManifestHttpClient).fetchBody(anyString())

    performAuthPost(appsUrl(), registerBody()).andIsOk
    val installId = appInstallService.findAll(testData.organization.id).single().id
    performAuthPost("${appsUrl()}/$installId/refresh", emptyMap<String, Any>()).andIsOk

    transactionActiveDuringFetch.assert.containsExactly(false, false)
  }

  @Test
  fun `rejects a manifest whose baseUrl is not an absolute http url`() {
    mockManifest(validManifest().replace("\"https://app.example.com\"", "\"not-a-url\""))
    performAuthPost(appsUrl(), registerBody()).andIsBadRequest.andAssertThatJson {
      node("code").isEqualTo("app_manifest_invalid")
      node("params[0]").isEqualTo("baseUrl must be an absolute http(s) URL")
    }
  }

  @Test
  fun `manifest url update without scope widening withholds newly declared scopes and drops removed ones`() {
    mockManifest(validManifest())
    performAuthPost(appsUrl(), registerBody()).andIsOk
    val installId = appInstallService.findAll(testData.organization.id).single().id

    mockManifest(validManifestV2WithExtraScope().replace("\"keys.edit\", ", ""))
    appInstallService.updateManifestUrl(
      organizationId = testData.organization.id,
      installId = installId,
      manifestUrl = AppsTestFixtures.MANIFEST_URL,
      allowScopeWidening = false,
    )

    appInstallService
      .find(testData.organization.id, installId)!!
      .grantedScopes.assert
      .containsExactlyInAnyOrder(Scope.TRANSLATIONS_VIEW)
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
  fun `updates the manifest URL and refetches from the new location`() {
    mockManifest(validManifest())
    performAuthPost(appsUrl(), registerBody()).andIsOk
    val installId = appInstallService.findAll(testData.organization.id).single().id

    mockManifest(validManifestV2())
    performAuthPatch(
      "${appsUrl()}/$installId/manifest-url",
      mapOf("manifestUrl" to "https://example.com/new-location/manifest.json"),
    ).andIsOk.andAssertThatJson {
      node("version").isEqualTo("0.2.0")
      node("modules.project-dashboard-page[0].title").isEqualTo("Home v2")
    }

    val install = appInstallService.find(testData.organization.id, installId)!!
    install.manifestUrl.assert.isEqualTo("https://example.com/new-location/manifest.json")
  }

  @Test
  fun `manifest URL update rejects manifest whose app id changed`() {
    mockManifest(validManifest())
    performAuthPost(appsUrl(), registerBody()).andIsOk
    val installId = appInstallService.findAll(testData.organization.id).single().id

    mockManifest(validManifest().replace("\"test-app\"", "\"different-app\""))
    performAuthPatch(
      "${appsUrl()}/$installId/manifest-url",
      mapOf("manifestUrl" to "https://example.com/new-location/manifest.json"),
    ).andIsBadRequest.andAssertThatJson {
      node("code").isEqualTo("app_manifest_invalid")
    }
  }

  @Test
  fun `removes a registered app`() {
    mockManifest(validManifest())
    performAuthPost(appsUrl(), registerBody()).andIsOk
    val installId = appInstallService.findAll(testData.organization.id).single().id

    performAuthDelete("${appsUrl()}/$installId").andIsOk
    appInstallService.findAll(testData.organization.id).assert.isEmpty()
  }

  private fun appsUrl() = "/v2/organizations/${testData.organization.id}/apps"

  private fun registerBody() = mapOf("manifestUrl" to AppsTestFixtures.MANIFEST_URL)

  private fun performAuthPatch(
    url: String,
    body: Any,
  ): ResultActions {
    loginAsAdminIfNotLogged()
    return perform(
      AuthorizedRequestFactory
        .addToken(patch(url))
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(body)),
    )
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

  private fun manifestWithExtraModule(): String =
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
        "key-action": [
          {"key": "view-source", "type": "link", "urlTemplate": "https://example.com/{keyName}"}
        ]
      }
    }
    """.trimIndent()

  private fun manifestWithoutDashboardPage(): String =
    """
    {
      "id": "test-app",
      "name": "Test App",
      "version": "0.1.0",
      "baseUrl": "https://app.example.com",
      "scopes": ["translations.view", "keys.edit"],
      "modules": {}
    }
    """.trimIndent()
}
