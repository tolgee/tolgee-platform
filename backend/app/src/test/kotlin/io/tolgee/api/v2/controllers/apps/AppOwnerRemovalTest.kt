package io.tolgee.api.v2.controllers.apps

import io.tolgee.development.testDataBuilder.data.NativeAppsTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsNotFound
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.node
import io.tolgee.repository.apps.AppRepository
import io.tolgee.service.apps.AppInstallService
import io.tolgee.service.apps.AppManifestHttpClient
import io.tolgee.service.apps.AppSecretService
import io.tolgee.service.apps.AppsTestFixtures
import io.tolgee.service.apps.lifecycle.AppLifecycleHttpClient
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.bean.override.mockito.MockitoBean
import tools.jackson.databind.JsonNode

/**
 * The publisher taking their app off the shelf. Distinct from one organization uninstalling: this
 * reaches every organization that installed the app and revokes its credentials in one operation,
 * which is what makes a compromised release recoverable without going tenant by tenant.
 */
class AppOwnerRemovalTest : AuthorizedControllerTest() {
  @Autowired
  lateinit var appInstallService: AppInstallService

  @Autowired
  lateinit var appSecretService: AppSecretService

  @Autowired
  lateinit var appRepository: AppRepository

  @MockitoBean
  @Autowired
  lateinit var appManifestHttpClient: AppManifestHttpClient

  @MockitoBean
  @Autowired
  lateinit var appLifecycleHttpClient: AppLifecycleHttpClient

  lateinit var testData: NativeAppsTestData
  var appEntityId: Long = 0
  var ownerInstallId: Long = 0
  var otherInstallId: Long = 0

  @BeforeEach
  fun setup() {
    testData = NativeAppsTestData()
    testDataService.saveTestData(testData.root)
    AppsTestFixtures.mockManifest(appManifestHttpClient)

    userAccount = testData.user
    val registered =
      postJson(
        "/v2/organizations/${testData.organization.id}/apps/register",
        mapOf("manifestUrl" to AppsTestFixtures.MANIFEST_URL),
      )
    appEntityId = registered.at("/app/id").asLong()
    ownerInstallId = registered.get("id").asLong()
    performAuthPut("/v2/projects/${testData.project.id}/apps/$ownerInstallId", null).andIsOk

    userAccount = testData.otherOwner
    otherInstallId =
      postJson(
        "/v2/organizations/${testData.otherOrganization.id}/apps",
        mapOf("manifestUrl" to AppsTestFixtures.MANIFEST_URL),
      ).get("id").asLong()
  }

  @AfterEach
  fun cleanup() {
    AppsTestFixtures.removeNativeInstalls(appInstallService)
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `the owner removes the app from every organization and its credentials go with it`() {
    userAccount = testData.user
    performAuthDelete("${ownedAppsUrl()}/$appEntityId").andIsOk

    executeInNewTransaction {
      appRepository.findByAppId("test-app").assert.isNull()
      appSecretService.list(appEntityId).assert.isEmpty()
    }
    appInstallService.findAll(testData.organization.id).assert.isEmpty()
    appInstallService.findAll(testData.otherOrganization.id).assert.isEmpty()
  }

  /** An installing organization removing its own install must not take the app from anyone else. */
  @Test
  fun `an installing organization uninstalling only removes itself`() {
    userAccount = testData.otherOwner
    performAuthDelete("/v2/organizations/${testData.otherOrganization.id}/apps/$otherInstallId").andIsOk

    appInstallService.findAll(testData.otherOrganization.id).assert.isEmpty()
    appInstallService.findAll(testData.organization.id).assert.hasSize(1)
    executeInNewTransaction { appRepository.findByAppId("test-app").assert.isNotNull }
  }

  @Test
  fun `an organization that merely installed the app cannot remove it everywhere`() {
    userAccount = testData.otherOwner
    performAuthDelete("/v2/organizations/${testData.otherOrganization.id}/owned-apps/$appEntityId")
      .andIsNotFound

    appInstallService.findAll(testData.organization.id).assert.hasSize(1)
  }

  @Test
  fun `the owner sees the app it registered, with how many organizations hold it`() {
    userAccount = testData.user
    performAuthGet(ownedAppsUrl()).andIsOk.andAssertThatJson {
      node("_embedded.ownedApps").isArray.hasSize(1)
      node("_embedded.ownedApps[0].appId").isEqualTo("test-app")
      node("_embedded.ownedApps[0].installCount").isEqualTo(2)
      node("_embedded.ownedApps[0].unhealthySince").isNull()
    }
  }

  private fun postJson(
    url: String,
    body: Map<String, Any?>,
  ): JsonNode {
    return objectMapper.readTree(
      performAuthPost(url, body)
        .andIsOk
        .andReturn()
        .response.contentAsString,
    )
  }

  private fun ownedAppsUrl() = "/v2/organizations/${testData.organization.id}/owned-apps"
}
