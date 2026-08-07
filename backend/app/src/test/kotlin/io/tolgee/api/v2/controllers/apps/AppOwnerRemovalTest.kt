package io.tolgee.api.v2.controllers.apps

import io.tolgee.development.testDataBuilder.data.NativeAppsTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsNotFound
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.node
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.model.apps.AppLifecycleEventType
import io.tolgee.repository.apps.AppRepository
import io.tolgee.service.apps.AppInstallService
import io.tolgee.service.apps.AppManifestHttpClient
import io.tolgee.service.apps.AppSecretService
import io.tolgee.service.apps.AppsTestFixtures
import io.tolgee.service.apps.lifecycle.AppLifecycleDeliveryService
import io.tolgee.service.apps.lifecycle.AppLifecycleHttpClient
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.clearInvocations
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.bean.override.mockito.MockitoBean
import tools.jackson.databind.JsonNode

/**
 * The publisher taking their app off the shelf. Distinct from one organization uninstalling: this
 * has to reach every organization that installed the app, and each of them has to be announced to
 * the app, or an app publisher cutting off a compromised release has no way to know who was cut off.
 */
class AppOwnerRemovalTest : AuthorizedControllerTest() {
  @Autowired
  lateinit var appInstallService: AppInstallService

  @Autowired
  lateinit var appSecretService: AppSecretService

  @Autowired
  lateinit var appRepository: AppRepository

  @Autowired
  lateinit var appLifecycleDeliveryService: AppLifecycleDeliveryService

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
    clearInvocations(appLifecycleHttpClient)
  }

  @AfterEach
  fun cleanup() {
    AppsTestFixtures.removeNativeInstalls(appInstallService)
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `the owner removes the app from every organization and both layers of credentials go with it`() {
    userAccount = testData.user
    performAuthDelete("${ownedAppsUrl()}/$appEntityId").andIsOk

    executeInNewTransaction {
      appRepository.findByAppId("test-app").assert.isNull()
      appSecretService.list(appEntityId).assert.isEmpty()
    }
    appInstallService.findAll(testData.organization.id).assert.isEmpty()
    appInstallService.findAll(testData.otherOrganization.id).assert.isEmpty()
  }

  /**
   * The deliveries outlive the app they announce the removal of, which is the point: an owner has to
   * be able to see afterwards whether the app was told.
   */
  @Test
  fun `every organization the app was installed in gets its own uninstalled delivery`() {
    userAccount = testData.user
    performAuthDelete("${ownedAppsUrl()}/$appEntityId").andIsOk

    executeInNewTransaction {
      val uninstalled =
        appLifecycleDeliveryService
          .listForApp("test-app")
          .filter { it.eventType == AppLifecycleEventType.APP_UNINSTALLED }
      uninstalled.map { it.organization?.id }.assert.containsExactlyInAnyOrder(
        testData.organization.id,
        testData.otherOrganization.id,
      )
      uninstalled.map { it.app }.assert.containsOnlyNulls()
      uninstalled.map { it.appIdentifier }.assert.containsOnly("test-app")
    }

    waitForNotThrowing(throwableClass = AssertionError::class, timeout = 10000) {
      val captor = argumentCaptor<String>()
      verify(appLifecycleHttpClient, atLeastOnce()).post(any(), captor.capture(), any())
      val organizationIds =
        captor.allValues
          .map { objectMapper.readTree(it) }
          .filter { it.get("eventType").asText() == "app.uninstalled" }
          .map { it.at("/organization/id").asLong() }
      organizationIds.assert.contains(testData.organization.id, testData.otherOrganization.id)
    }
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
