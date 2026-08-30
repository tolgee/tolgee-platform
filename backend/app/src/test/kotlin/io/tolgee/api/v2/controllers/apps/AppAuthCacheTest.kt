package io.tolgee.api.v2.controllers.apps

import io.tolgee.constants.Caches
import io.tolgee.development.testDataBuilder.data.AppsTestData
import io.tolgee.dtos.cacheable.AppDto
import io.tolgee.dtos.cacheable.AppInstallDto
import io.tolgee.fixtures.andIsOk
import io.tolgee.service.apps.AppManifestHttpClient
import io.tolgee.service.apps.AppsTestFixtures
import io.tolgee.service.apps.lifecycle.AppLifecycleHttpClient
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.ContextRecreatingTest
import io.tolgee.testing.assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Duration

/**
 * The app-auth caches must never outlive the security decision they cache: a force-revoke, an
 * uninstall, or a scope reduction has to take effect on the very next request, not two hours later.
 * These run with the cache on so a missed eviction is a red test.
 */
@ContextRecreatingTest
@SpringBootTest(
  properties = [
    "tolgee.cache.enabled=true",
  ],
)
@AutoConfigureMockMvc
class AppAuthCacheTest : AuthorizedControllerTest() {
  @MockitoBean
  @Autowired
  lateinit var appManifestHttpClient: AppManifestHttpClient

  @MockitoBean
  @Autowired
  lateinit var appLifecycleHttpClient: AppLifecycleHttpClient

  lateinit var testData: AppsTestData
  lateinit var appClientId: String
  lateinit var appClientSecret: String
  var appEntityId: Long = 0
  var installId: Long = 0

  @BeforeEach
  fun setup() {
    testData = AppsTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    AppsTestFixtures.mockManifest(appManifestHttpClient, manifest(""""translations.view""""))

    val json = objectMapper.readTree(register())
    installId = json.get("installId").asLong()
    appEntityId = json.get("id").asLong()
    appClientId = json.get("clientId").asText()
    appClientSecret = json.get("clientSecret").asText()

    performAuthPut("/v2/projects/${testData.project.id}/apps/$installId", null).andIsOk

    clearCaches()
  }

  @AfterEach
  fun cleanup() {
    currentDateProvider.forcedDate = null
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `force-revoke kills already-minted app and install tokens on the next request`() {
    val installToken = mintInstallToken()
    val appLevelToken = mintAppLevelToken()

    translationsWith(installToken).andIsOk
    installationsWith(appLevelToken).andIsOk

    // Both requests loaded the app snapshot into the APPS cache with no cutoff.
    (cachedApp()!!.tokensInvalidBefore).assert.isNull()

    issueSecret()
    currentDateProvider.move(Duration.ofSeconds(2))
    forceRevoke(firstSecretId())

    cachedApp().assert.isNull()

    translationsWith(installToken).andExpect(status().isUnauthorized)
    installationsWith(appLevelToken).andExpect(status().isUnauthorized)
  }

  @Test
  fun `uninstall stops an already-minted install token on the next request`() {
    val installToken = mintInstallToken()
    translationsWith(installToken).andIsOk
    cachedInstall().assert.isNotNull

    loginAsUser()
    performAuthDelete("/v2/organizations/${testData.organization.id}/apps/$installId").andIsOk

    cachedInstall().assert.isNull()

    translationsWith(installToken).andExpect(status().isUnauthorized)
  }

  @Test
  fun `an owner refresh that drops a scope is enforced on the next request`() {
    val installToken = mintInstallToken()
    translationsWith(installToken).andIsOk
    cachedInstall()!!
      .grantedScopes
      .map { it.value }
      .assert
      .containsExactly("translations.view")

    AppsTestFixtures.mockManifest(appManifestHttpClient, manifest(""""activity.view""""))
    loginAsUser()
    performAuthPost("/v2/organizations/${testData.organization.id}/apps/$installId/refresh", null).andIsOk

    cachedInstall().assert.isNull()

    translationsWith(installToken).andExpect(status().isForbidden)
  }

  private fun cachedApp(): AppDto? = cacheManager.getCache(Caches.APPS)!!.get(appEntityId)?.get() as AppDto?

  private fun cachedInstall(): AppInstallDto? =
    cacheManager.getCache(Caches.APP_INSTALLS)!!.get(installId)?.get() as AppInstallDto?

  private fun register(): String =
    performAuthPost(
      "/v2/organizations/${testData.organization.id}/owned-apps",
      mapOf("manifestUrl" to AppsTestFixtures.MANIFEST_URL),
    ).andIsOk.andReturn().response.contentAsString

  private fun mintInstallToken(): String = mintToken(installId)

  private fun mintAppLevelToken(): String = mintToken(null)

  private fun mintToken(installId: Long?): String {
    logout()
    val body =
      mutableMapOf<String, Any>(
        "grant_type" to "client_credentials",
        "client_id" to appClientId,
        "client_secret" to appClientSecret,
      )
    installId?.let { body["install_id"] = it }
    val response =
      perform(
        post("/v2/public/apps/token")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(body)),
      ).andIsOk.andReturn().response.contentAsString
    userAccount = testData.user
    return objectMapper.readTree(response).get("access_token").asText()
  }

  private fun issueSecret() {
    loginAsUser()
    performAuthPost(
      "/v2/organizations/${testData.organization.id}/owned-apps/$appEntityId/secrets/rotate",
      mapOf("graceSeconds" to 86_400L),
    ).andIsOk
  }

  private fun firstSecretId(): Long {
    loginAsUser()
    val response =
      performAuthGet("/v2/organizations/${testData.organization.id}/owned-apps/$appEntityId/secrets")
        .andIsOk
        .andReturn()
        .response.contentAsString
    val secrets = objectMapper.readTree(response).get("_embedded").get("appSecrets")
    return secrets.minByOrNull { it.get("createdAt").asLong() }!!.get("id").asLong()
  }

  private fun forceRevoke(secretId: Long) {
    loginAsUser()
    performAuthDelete(
      "/v2/organizations/${testData.organization.id}/owned-apps/$appEntityId/secrets/$secretId?force=true",
    ).andIsOk
  }

  private fun loginAsUser() {
    userAccount = testData.user
  }

  private fun translationsWith(token: String): ResultActions {
    logout()
    return perform(
      get("/v2/projects/${testData.project.id}/translations")
        .header(HttpHeaders.AUTHORIZATION, "Bearer $token"),
    )
  }

  private fun installationsWith(token: String): ResultActions {
    logout()
    return perform(
      get("/v2/apps/self/installations").header(HttpHeaders.AUTHORIZATION, "Bearer $token"),
    )
  }

  private fun manifest(scopes: String): String =
    """
    {
      "id": "test-app",
      "name": "Test App",
      "version": "0.1.0",
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
