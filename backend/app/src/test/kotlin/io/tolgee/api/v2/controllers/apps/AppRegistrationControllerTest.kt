package io.tolgee.api.v2.controllers.apps

import io.tolgee.development.testDataBuilder.data.NativeAppsTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsNotFound
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.node
import io.tolgee.repository.apps.AppRepository
import io.tolgee.service.apps.AppAvailabilityService
import io.tolgee.service.apps.AppInstallService
import io.tolgee.service.apps.AppManifestHttpClient
import io.tolgee.service.apps.AppSecretService
import io.tolgee.service.apps.AppService
import io.tolgee.service.apps.AppsTestFixtures
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.assert
import io.tolgee.util.executeInNewTransaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.bean.override.mockito.MockitoBean

/**
 * Covers the layer an app publisher lives in: one app registered once, installed by many
 * organizations, with app-level credentials that only its owner ever sees.
 */
class AppRegistrationControllerTest : AuthorizedControllerTest() {
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

  @BeforeEach
  fun setup() {
    testData = NativeAppsTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    AppsTestFixtures.mockManifest(appManifestHttpClient)
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `installing a manifest nobody registered is refused with its own outcome`() {
    performAuthPost(installUrl(testData.organization.id), manifestBody())
      .andIsNotFound
      .andAssertThatJson {
        node("code").isEqualTo("app_not_registered")
        node("params[0]").isEqualTo("test-app")
      }

    appInstallService.findAll(testData.organization.id).assert.isEmpty()
    appRepository.findByAppId("test-app").assert.isNull()
  }

  @Test
  fun `registering creates the app, owns it and installs it in one go`() {
    val response = register(testData.organization.id)

    response
      .at("/clientId")
      .asText()
      .assert
      .startsWith(AppService.APP_CLIENT_ID_PREFIX)
    response
      .at("/clientSecret")
      .asText()
      .assert
      .startsWith(AppService.APP_CLIENT_SECRET_PREFIX)
    response
      .at("/webhookSecret")
      .asText()
      .assert
      .isNotBlank()
    response
      .at("/installId")
      .asLong()
      .assert
      .isNotEqualTo(0L)

    ownerOrganizationIdOf("test-app").assert.isEqualTo(testData.organization.id)
    appSecretService.list(appRepository.findByAppId("test-app")!!.id).assert.hasSize(1)
    appInstallService.findAll(testData.organization.id).assert.hasSize(1)
  }

  @Test
  fun `registering without install registers the app but creates no install`() {
    performAuthPost(ownedUrl(testData.organization.id), manifestBody() + mapOf("install" to false))
      .andIsOk
      .andAssertThatJson {
        node("clientId").isString.startsWith(AppService.APP_CLIENT_ID_PREFIX)
        node("installId").isNull()
      }

    ownerOrganizationIdOf("test-app").assert.isEqualTo(testData.organization.id)
    appInstallService.findAll(testData.organization.id).assert.isEmpty()
  }

  @Test
  fun `a failed install leaves no app registered behind`() {
    AppsTestFixtures.mockManifest(appManifestHttpClient, MANIFEST_WITH_UNKNOWN_SCOPE)

    performAuthPost(ownedUrl(testData.organization.id), manifestBody()).andIsBadRequest

    appRepository.findByAppId("test-app").assert.isNull()
  }

  @Test
  fun `app-level credentials are disclosed once and never again`() {
    register(testData.organization.id)

    performAuthGet(installUrl(testData.organization.id)).andIsOk.andAssertThatJson {
      node("_embedded.appInstalls[0].clientId").isAbsent()
      node("_embedded.appInstalls[0].clientSecret").isAbsent()
      node("_embedded.appInstalls[0].app").isAbsent()
    }
  }

  @Test
  fun `two organizations installing the same manifest get one app and two installs`() {
    val first = register(testData.organization.id)
    appAvailabilityService.setAvailableToAll(first.at("/id").asLong())

    userAccount = testData.otherOwner
    val secondInstallId =
      performAuthPost(installUrl(testData.otherOrganization.id), manifestBody())
        .andIsOk
        .andReturn()
        .response.contentAsString
        .let { objectMapper.readTree(it) }
        .at("/id")
        .asLong()

    secondInstallId.assert.isNotEqualTo(first.at("/installId").asLong())
    appInstallService.findAll(testData.otherOrganization.id).assert.hasSize(1)
    appInstallService.findAll(testData.organization.id).assert.hasSize(1)
    appRepository
      .findAll()
      .filter { it.appId == "test-app" }
      .assert
      .hasSize(1)
  }

  @Test
  fun `an organization installing somebody else's app never sees any credentials`() {
    appAvailabilityService.setAvailableToAll(register(testData.organization.id).at("/id").asLong())

    userAccount = testData.otherOwner
    performAuthPost(installUrl(testData.otherOrganization.id), manifestBody()).andIsOk.andAssertThatJson {
      node("clientId").isAbsent()
      node("clientSecret").isAbsent()
      node("webhookSecret").isAbsent()
      node("app").isAbsent()
    }
  }

  /**
   * Registering is only ever for a new app. Registering an app somebody already registered is
   * refused: installing an existing app is the separate install endpoint, and register must never
   * hand a second organization ownership or credentials of an app it did not publish.
   */
  @Test
  fun `registering an already-registered app is refused`() {
    appAvailabilityService.setAvailableToAll(register(testData.organization.id).at("/id").asLong())

    userAccount = testData.otherOwner
    performAuthPost(ownedUrl(testData.otherOrganization.id), manifestBody())
      .andIsBadRequest
      .andAssertThatJson { node("code").isEqualTo("app_already_registered") }

    ownerOrganizationIdOf("test-app").assert.isEqualTo(testData.organization.id)
    appInstallService.findAll(testData.otherOrganization.id).assert.isEmpty()
  }

  @Test
  fun `installing an app another organization owns is refused until it is made available`() {
    val appEntityId = register(testData.organization.id).at("/id").asLong()

    userAccount = testData.otherOwner
    performAuthPost(installUrl(testData.otherOrganization.id), manifestBody())
      .andIsForbidden
      .andAssertThatJson { node("code").isEqualTo("app_not_available_for_organization") }
    appInstallService.findAll(testData.otherOrganization.id).assert.isEmpty()

    appAvailabilityService.setAvailableToAll(appEntityId)

    performAuthPost(installUrl(testData.otherOrganization.id), manifestBody()).andIsOk
    appInstallService.findAll(testData.otherOrganization.id).assert.hasSize(1)
  }

  @Test
  fun `a specific-organization grant lets exactly that organization install`() {
    val appEntityId = register(testData.organization.id).at("/id").asLong()
    appAvailabilityService.addAvailableOrganization(appEntityId, testData.otherOrganization.id)

    userAccount = testData.otherOwner
    performAuthPost(installUrl(testData.otherOrganization.id), manifestBody()).andIsOk
    appInstallService.findAll(testData.otherOrganization.id).assert.hasSize(1)
  }

  private fun ownerOrganizationIdOf(appId: String): Long? =
    executeInNewTransaction(platformTransactionManager) {
      appRepository.findByAppId(appId)!!.organization.id
    }

  private fun register(organizationId: Long) =
    objectMapper.readTree(
      performAuthPost(ownedUrl(organizationId), manifestBody())
        .andIsOk
        .andReturn()
        .response.contentAsString,
    )

  private fun installUrl(organizationId: Long) = "/v2/organizations/$organizationId/apps"

  private fun ownedUrl(organizationId: Long) = "/v2/organizations/$organizationId/owned-apps"

  private fun manifestBody() = mapOf("manifestUrl" to AppsTestFixtures.MANIFEST_URL)

  companion object {
    private val MANIFEST_WITH_UNKNOWN_SCOPE =
      AppsTestFixtures.MANIFEST.replace(
        "\"modules\"",
        "\"scopes\": [\"not.a.real.scope\"],\n\"modules\"",
      )
  }
}
