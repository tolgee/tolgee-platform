package io.tolgee.api.v2.controllers.organization

import io.tolgee.constants.Message
import io.tolgee.development.testDataBuilder.data.AppsTestData
import io.tolgee.exceptions.BadRequestException
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsNotFound
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.node
import io.tolgee.model.enums.Scope
import io.tolgee.service.apps.AppInstallService
import io.tolgee.service.apps.AppManifestHttpClient
import io.tolgee.service.apps.AppService
import io.tolgee.service.apps.AppsTestFixtures
import io.tolgee.service.apps.lifecycle.AppLifecycleHttpClient
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
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.transaction.support.TransactionSynchronizationManager

class OrganizationAppsControllerTest : AuthorizedControllerTest() {
  @Autowired
  lateinit var appInstallService: AppInstallService

  @Autowired
  lateinit var appService: AppService

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
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `registers an app from a valid manifest`() {
    mockManifest(validManifest())
    performAuthPost(registerUrl(), registerBody()).andIsOk.andAssertThatJson {
      node("appId").isEqualTo("test-app")
      node("name").isEqualTo("Test App")
      node("clientId").isString.startsWith(AppService.APP_CLIENT_ID_PREFIX)
      node("installId").isNumber
    }
    val install = appInstallService.findAll(testData.organization.id).single()
    install.grantedScopes.assert.containsExactlyInAnyOrder(Scope.TRANSLATIONS_VIEW, Scope.KEYS_EDIT)
    // The manifest details are exposed on the install listing, not on the registration response.
    performAuthGet(appsUrl()).andIsOk.andAssertThatJson {
      node("_embedded.appInstalls[0].version").isEqualTo("0.1.0")
      node("_embedded.appInstalls[0].baseUrl").isEqualTo("https://app.example.com")
      node("_embedded.appInstalls[0].modules.project-dashboard-page[0].key").isEqualTo("home")
      node("_embedded.appInstalls[0].scopes").isArray.containsExactlyInAnyOrder("translations.view", "keys.edit")
    }
    // Registering read the manifest, so it already counts as the first health check.
    executeInNewTransaction {
      appService
        .find("test-app")!!
        .manifestLastCheckedAt.assert.isNotNull
    }
  }

  @Test
  fun `stores a relative image icon resolved against the base url`() {
    mockManifest(manifestWithIcon("/assets/logo.svg"))
    performAuthPost(registerUrl(), registerBody()).andIsOk
    performAuthGet(appsUrl()).andIsOk.andAssertThatJson {
      node("_embedded.appInstalls[0].icon").isEqualTo("https://app.example.com/assets/logo.svg")
    }
  }

  @Test
  fun `passes an emoji icon through`() {
    mockManifest(manifestWithIcon("🧩"))
    performAuthPost(registerUrl(), registerBody()).andIsOk
    performAuthGet(appsUrl()).andIsOk.andAssertThatJson {
      node("_embedded.appInstalls[0].icon").isEqualTo("🧩")
    }
  }

  @Test
  fun `rejects a non-http image icon`() {
    mockManifest(manifestWithIcon("file:///etc/passwd"))
    performAuthPost(registerUrl(), registerBody())
      .andIsBadRequest
      .andAssertThatJson { node("params[0]").isString.contains("icon") }
  }

  @Test
  fun `counts the projects the install is enabled for`() {
    mockManifest(validManifest())
    performAuthPost(registerUrl(), registerBody()).andIsOk
    val installId = appInstallService.findAll(testData.organization.id).single().id

    performAuthGet(appsUrl()).andIsOk.andAssertThatJson {
      node("_embedded.appInstalls[0].enabledProjectCount").isEqualTo(0)
    }

    performAuthPut("/v2/projects/${testData.project.id}/apps/$installId", null).andIsOk
    performAuthGet(appsUrl()).andIsOk.andAssertThatJson {
      node("_embedded.appInstalls[0].enabledProjectCount").isEqualTo(1)
    }
  }

  @Test
  fun `a fourth registered app is refused by the server-wide limit`() {
    (1..3).forEach { index ->
      mockManifest(manifestWithId("test-app-$index"))
      performAuthPost(registerUrl(), registerBody()).andIsOk
    }

    mockManifest(manifestWithId("test-app-4"))
    performAuthPost(registerUrl(), registerBody())
      .andIsBadRequest
      .andAssertThatJson { node("code").isEqualTo("plan_apps_limit_exceeded") }
  }

  @Test
  fun `registration discloses the app-level credentials exactly once`() {
    mockManifest(validManifest())
    performAuthPost(registerUrl(), registerBody()).andIsOk.andAssertThatJson {
      node("clientId").isString.startsWith(AppService.APP_CLIENT_ID_PREFIX)
      node("clientSecret").isString.startsWith(AppService.APP_CLIENT_SECRET_PREFIX)
    }

    performAuthGet(appsUrl()).andIsOk.andAssertThatJson {
      node("_embedded.appInstalls[0].clientSecret").isAbsent()
      node("_embedded.appInstalls[0].clientId").isAbsent()
    }
  }

  @Test
  fun `preview returns parsed manifest with requested scopes and a hash without persisting`() {
    mockManifest(validManifest())
    performAuthPost("${appsUrl()}/preview", registerBody()).andIsOk.andAssertThatJson {
      node("appId").isEqualTo("test-app")
      node("name").isEqualTo("Test App")
      node("version").isEqualTo("0.1.0")
      node("requestedScopes").isArray.containsExactlyInAnyOrder("translations.view", "keys.edit")
      node("manifestHash").isString.isNotEqualTo("")
    }
    appInstallService.findAll(testData.organization.id).assert.isEmpty()
  }

  @Test
  fun `rejects register when the manifest changed since the preview`() {
    mockManifest(validManifest())
    val hash =
      objectMapper
        .readTree(
          performAuthPost("${appsUrl()}/preview", registerBody())
            .andIsOk
            .andReturn()
            .response.contentAsString,
        ).at("/manifestHash")
        .asText()

    // The app now asks for one more scope than the preview showed - a bait-and-switch that must be refused.
    mockManifest(validManifest().replace("\"keys.edit\"", "\"keys.edit\", \"keys.create\""))
    performAuthPost(registerUrl(), registerBody() + mapOf("manifestHash" to hash))
      .andIsBadRequest
      .andAssertThatJson { node("code").isEqualTo("app_manifest_changed") }
    appService.find("test-app").assert.isNull()
  }

  @Test
  fun `accepts register when the hash still matches the manifest`() {
    mockManifest(validManifest())
    val hash =
      objectMapper
        .readTree(
          performAuthPost("${appsUrl()}/preview", registerBody())
            .andIsOk
            .andReturn()
            .response.contentAsString,
        ).at("/manifestHash")
        .asText()

    performAuthPost(registerUrl(), registerBody() + mapOf("manifestHash" to hash)).andIsOk
    appInstallService.findAll(testData.organization.id).assert.hasSize(1)
  }

  @Test
  fun `remove does not reach an install belonging to another organization`() {
    mockManifest(validManifest())
    performAuthPost(registerUrl(), registerBody()).andIsOk
    val installId = appInstallService.findAll(testData.organization.id).single().id

    val otherOrgId = testData.otherOrganization.id
    userAccount = testData.otherOwner

    performAuthDelete("/v2/organizations/$otherOrgId/apps/$installId").andIsNotFound

    appInstallService.find(testData.organization.id, installId).assert.isNotNull
  }

  @Test
  fun `every org apps endpoint rejects a non-owner org member`() {
    mockManifest(validManifest())
    performAuthPost(registerUrl(), registerBody()).andIsOk
    val installId = appInstallService.findAll(testData.organization.id).single().id

    userAccount = testData.member

    performAuthGet(appsUrl()).andIsForbidden
    performAuthPost(appsUrl(), registerBody()).andIsForbidden
    performAuthPost(registerUrl(), registerBody()).andIsForbidden
    performAuthGet(ownedUrl()).andIsForbidden
    performAuthPost("${appsUrl()}/preview", registerBody()).andIsForbidden
    performAuthPost("${appsUrl()}/$installId/refresh", null).andIsForbidden
    performAuthDelete("${appsUrl()}/$installId").andIsForbidden
  }

  @Test
  fun `rejects a manifest URL pointing at a private address`() {
    performAuthPost(
      registerUrl(),
      mapOf("manifestUrl" to "http://127.0.0.1/manifest.json"),
    ).andIsBadRequest.andAssertThatJson {
      node("code").isEqualTo("url_not_valid")
    }
  }

  @Test
  fun `rejects manifest with an unknown scope`() {
    mockManifest(validManifest().replace("\"keys.edit\"", "\"not.a.real.scope\""))
    performAuthPost(registerUrl(), registerBody()).andIsBadRequest.andAssertThatJson {
      node("code").isEqualTo("app_manifest_invalid")
    }
  }

  @Test
  fun `rejects manifest with an organization-level scope`() {
    mockManifest(validManifest().replace("\"keys.edit\"", "\"organization-members.manage\""))
    performAuthPost(registerUrl(), registerBody()).andIsBadRequest.andAssertThatJson {
      node("code").isEqualTo("app_manifest_invalid")
      node("params[0]").isEqualTo("organization-level scope not allowed in manifest: organization-members.manage")
    }
  }

  @Test
  fun `register without scopes block stores no granted scopes`() {
    mockManifest(manifestWithoutScopes())
    performAuthPost(registerUrl(), registerBody()).andIsOk
    val install = appInstallService.findAll(testData.organization.id).single()
    install.grantedScopes.assert.isEmpty()
    performAuthGet(appsUrl()).andIsOk.andAssertThatJson {
      node("_embedded.appInstalls[0].scopes").isArray.isEmpty()
    }
  }

  @Test
  fun `rejects a manifest declaring an unsupported top-level decoratorsUrl feature`() {
    mockManifest(
      validManifest().replace(
        "\"scopes\":",
        "\"decoratorsUrl\": \"https://app.example.com/d\",\n\"scopes\":",
      ),
    )
    performAuthPost(registerUrl(), registerBody()).andIsBadRequest.andAssertThatJson {
      node("code").isEqualTo("app_manifest_invalid")
      node("params[0]").isEqualTo("unsupported manifest features: decoratorsUrl")
    }
  }

  @Test
  fun `rejects a manifest declaring a non-dashboard module`() {
    mockManifest(manifestWithExtraModule())
    performAuthPost(registerUrl(), registerBody()).andIsBadRequest.andAssertThatJson {
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
    performAuthPost(registerUrl(), registerBody()).andIsBadRequest.andAssertThatJson {
      node("code").isEqualTo("app_manifest_invalid")
      node("params[0]").isEqualTo("unsupported manifest features: key-action, webhooks")
    }
  }

  @Test
  fun `rejects a manifest with no dashboard page module`() {
    mockManifest(manifestWithoutDashboardPage())
    performAuthPost(registerUrl(), registerBody()).andIsBadRequest.andAssertThatJson {
      node("code").isEqualTo("app_manifest_invalid")
    }
  }

  @Test
  fun `rejects re-registering an app that is already registered`() {
    mockManifest(validManifest())
    performAuthPost(registerUrl(), registerBody()).andIsOk

    performAuthPost(registerUrl(), registerBody()).andIsBadRequest.andAssertThatJson {
      node("code").isEqualTo("app_already_registered")
    }
  }

  @Test
  fun `rejects invalid manifest JSON`() {
    mockManifest("not valid json")
    performAuthPost(registerUrl(), registerBody()).andIsBadRequest.andAssertThatJson {
      node("code").isEqualTo("app_manifest_invalid")
    }
  }

  @Test
  fun `rejects unreachable manifest URL`() {
    doThrow(BadRequestException(Message.APP_MANIFEST_FETCH_FAILED))
      .whenever(appManifestHttpClient)
      .fetchBody(anyString())

    performAuthPost(registerUrl(), registerBody()).andIsBadRequest.andAssertThatJson {
      node("code").isEqualTo("app_manifest_fetch_failed")
    }
  }

  @Test
  fun `rejects a blank manifest URL with a validation error`() {
    performAuthPost(registerUrl(), mapOf("manifestUrl" to "")).andIsBadRequest.andAssertThatJson {
      node("STANDARD_VALIDATION.manifestUrl").isNotNull()
    }
  }

  @Test
  fun `rejects a manifest whose fields exceed the stored column length`() {
    mockManifest(validManifest().replace("\"Test App\"", "\"${"x".repeat(256)}\""))
    performAuthPost(registerUrl(), registerBody()).andIsBadRequest.andAssertThatJson {
      node("code").isEqualTo("app_manifest_invalid")
    }
  }

  @Test
  fun `rejects a manifest with a blank required field`() {
    mockManifest(validManifest().replace("\"Test App\"", "\"  \""))
    performAuthPost(registerUrl(), registerBody()).andIsBadRequest.andAssertThatJson {
      node("code").isEqualTo("app_manifest_invalid")
    }
  }

  @Test
  fun `rejects a manifest whose module entry does not resolve to an http url`() {
    mockManifest(validManifest().replace("\"entry\": \"/\"", "\"entry\": \"javascript:alert(1)\""))
    performAuthPost(registerUrl(), registerBody()).andIsBadRequest.andAssertThatJson {
      node("code").isEqualTo("app_manifest_invalid")
    }
  }

  /** Pins the no-transaction invariant documented on [io.tolgee.service.apps.AppInstallService]. */
  @Test
  fun `does not hold a transaction open while fetching the manifest`() {
    val transactionActiveDuringFetch = mutableListOf<Boolean>()
    doAnswer {
      transactionActiveDuringFetch.add(TransactionSynchronizationManager.isActualTransactionActive())
      validManifest()
    }.whenever(appManifestHttpClient).fetchBody(anyString())

    performAuthPost(registerUrl(), registerBody()).andIsOk

    transactionActiveDuringFetch.assert.containsExactly(false)
  }

  @Test
  fun `rejects a manifest whose baseUrl is not an absolute http url`() {
    mockManifest(validManifest().replace("\"https://app.example.com\"", "\"not-a-url\""))
    performAuthPost(registerUrl(), registerBody()).andIsBadRequest.andAssertThatJson {
      node("code").isEqualTo("app_manifest_invalid")
      node("params[0]").isEqualTo("baseUrl must be an absolute http(s) URL")
    }
  }

  @Test
  fun `lists installed apps for the organization`() {
    mockManifest(validManifest())
    performAuthPost(registerUrl(), registerBody()).andIsOk

    performAuthGet(appsUrl()).andIsOk.andAssertThatJson {
      node("_embedded.appInstalls").isArray.hasSize(1)
      node("_embedded.appInstalls[0].appId").isEqualTo("test-app")
    }
  }

  @Test
  fun `removes an installed app`() {
    mockManifest(validManifest())
    performAuthPost(registerUrl(), registerBody()).andIsOk
    val installId = appInstallService.findAll(testData.organization.id).single().id

    performAuthDelete("${appsUrl()}/$installId").andIsOk
    appInstallService.findAll(testData.organization.id).assert.isEmpty()
  }

  private fun appsUrl() = "/v2/organizations/${testData.organization.id}/apps"

  private fun ownedUrl() = "/v2/organizations/${testData.organization.id}/owned-apps"

  private fun registerUrl() = ownedUrl()

  private fun registerBody() = mapOf("manifestUrl" to AppsTestFixtures.MANIFEST_URL)

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

  private fun manifestWithIcon(icon: String): String =
    """
    {
      "id": "test-app",
      "name": "Test App",
      "version": "0.1.0",
      "baseUrl": "https://app.example.com",
      "icon": "$icon",
      "modules": {
        "project-dashboard-page": [
          {"key": "home", "title": "Home", "icon": "🏠", "entry": "/"}
        ]
      }
    }
    """.trimIndent()

  private fun manifestWithId(id: String): String =
    """
    {
      "id": "$id",
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
