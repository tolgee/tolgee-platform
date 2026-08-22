package io.tolgee.api.v2.controllers.apps

import io.tolgee.development.testDataBuilder.data.NativeAppsTestData
import io.tolgee.fixtures.andIsNotFound
import io.tolgee.fixtures.andIsOk
import io.tolgee.repository.apps.AppRepository
import io.tolgee.service.apps.AppAvailabilityService
import io.tolgee.service.apps.AppInstallService
import io.tolgee.service.apps.AppManifestHttpClient
import io.tolgee.service.apps.AppSecretService
import io.tolgee.service.apps.AppsTestFixtures
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.assert
import io.tolgee.util.executeInNewTransaction
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
class OwnedAppRemovalTest : AuthorizedControllerTest() {
  @Autowired
  lateinit var appInstallService: AppInstallService

  @Autowired
  lateinit var appSecretService: AppSecretService

  @Autowired
  lateinit var appAvailabilityService: AppAvailabilityService

  @Autowired
  lateinit var appRepository: AppRepository

  @MockitoBean
  @Autowired
  lateinit var appManifestHttpClient: AppManifestHttpClient

  lateinit var testData: NativeAppsTestData
  var appEntityId: Long = 0
  var otherInstallId: Long = 0

  @BeforeEach
  fun setup() {
    testData = NativeAppsTestData()
    testDataService.saveTestData(testData.root)
    AppsTestFixtures.mockManifest(appManifestHttpClient)

    userAccount = testData.user
    val registered = register(testData.organization.id)
    appEntityId = registered.at("/id").asLong()
    appAvailabilityService.setAvailableToAll(appEntityId)

    userAccount = testData.otherOwner
    otherInstallId = install(testData.otherOrganization.id).at("/id").asLong()
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `the owner removes the app from every organization and its credentials go with it`() {
    userAccount = testData.user
    performAuthDelete("${ownedUrl(testData.organization.id)}/$appEntityId").andIsOk

    executeInNewTransaction {
      appRepository.findByAppId("test-app").assert.isNull()
      appSecretService.list(appEntityId).assert.isEmpty()
    }
    appInstallService.findAll(testData.organization.id).assert.isEmpty()
    appInstallService.findAll(testData.otherOrganization.id).assert.isEmpty()
  }

  @Test
  fun `an organization uninstalling only removes itself`() {
    userAccount = testData.otherOwner
    performAuthDelete("${installUrl(testData.otherOrganization.id)}/$otherInstallId").andIsOk

    appInstallService.findAll(testData.otherOrganization.id).assert.isEmpty()
    appInstallService.findAll(testData.organization.id).assert.hasSize(1)
    executeInNewTransaction { appRepository.findByAppId("test-app").assert.isNotNull }
  }

  @Test
  fun `an organization that merely installed the app cannot remove it everywhere`() {
    userAccount = testData.otherOwner
    performAuthDelete("${ownedUrl(testData.otherOrganization.id)}/$appEntityId").andIsNotFound

    appInstallService.findAll(testData.organization.id).assert.hasSize(1)
  }

  private fun register(organizationId: Long): JsonNode =
    objectMapper.readTree(
      performAuthPost(ownedUrl(organizationId), manifestBody())
        .andIsOk
        .andReturn()
        .response.contentAsString,
    )

  private fun install(organizationId: Long): JsonNode =
    objectMapper.readTree(
      performAuthPost(installUrl(organizationId), manifestBody())
        .andIsOk
        .andReturn()
        .response.contentAsString,
    )

  private fun installUrl(organizationId: Long) = "/v2/organizations/$organizationId/apps"

  private fun ownedUrl(organizationId: Long) = "/v2/organizations/$organizationId/owned-apps"

  private fun manifestBody() = mapOf("manifestUrl" to AppsTestFixtures.MANIFEST_URL)
}
