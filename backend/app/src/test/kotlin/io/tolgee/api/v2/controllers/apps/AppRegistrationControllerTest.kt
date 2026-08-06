package io.tolgee.api.v2.controllers.apps

import io.tolgee.development.testDataBuilder.data.NativeAppsTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsNotFound
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.node
import io.tolgee.repository.apps.AppRepository
import io.tolgee.service.apps.AppInstallService
import io.tolgee.service.apps.AppManifestHttpClient
import io.tolgee.service.apps.AppSecretService
import io.tolgee.service.apps.AppService
import io.tolgee.service.apps.AppsTestFixtures
import io.tolgee.service.apps.lifecycle.AppLifecycleHttpClient
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
  lateinit var appRepository: AppRepository

  @MockitoBean
  @Autowired
  lateinit var appManifestHttpClient: AppManifestHttpClient

  @MockitoBean
  @Autowired
  lateinit var appLifecycleHttpClient: AppLifecycleHttpClient

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
    AppsTestFixtures.removeNativeInstalls(appInstallService)
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
      .at("/app/clientId")
      .asText()
      .assert
      .startsWith(AppService.APP_CLIENT_ID_PREFIX)
    response
      .at("/app/clientSecret")
      .asText()
      .assert
      .startsWith(AppService.APP_CLIENT_SECRET_PREFIX)
    response
      .at("/app/webhookSecret")
      .asText()
      .assert
      .isNotBlank()
    response
      .at("/clientSecret")
      .asText()
      .assert
      .startsWith(AppInstallService.CLIENT_SECRET_PREFIX)

    ownerOrganizationIdOf("test-app").assert.isEqualTo(testData.organization.id)
    appSecretService.list(appRepository.findByAppId("test-app")!!.id).assert.hasSize(1)
    appInstallService.findAll(testData.organization.id).assert.hasSize(1)
  }

  @Test
  fun `a failed install leaves no app registered behind`() {
    AppsTestFixtures.mockManifest(appManifestHttpClient, MANIFEST_WITH_UNKNOWN_SCOPE)

    performAuthPost(registerUrl(testData.organization.id), manifestBody()).andIsBadRequest

    appRepository.findByAppId("test-app").assert.isNull()
  }

  @Test
  fun `app-level credentials are disclosed once and never again`() {
    register(testData.organization.id)

    performAuthGet(installUrl(testData.organization.id)).andIsOk.andAssertThatJson {
      node("_embedded.appInstalls[0].app").isNull()
    }
  }

  @Test
  fun `two organizations installing the same manifest get one app and two installs`() {
    val first = register(testData.organization.id)

    userAccount = testData.otherOwner
    val second =
      performAuthPost(installUrl(testData.otherOrganization.id), manifestBody())
        .andIsOk
        .andReturn()
        .response.contentAsString
        .let { objectMapper.readTree(it) }

    second
      .at("/app/id")
      .asLong()
      .assert
      .isEqualTo(first.at("/app/id").asLong())
    second
      .at("/id")
      .asLong()
      .assert
      .isNotEqualTo(first.at("/id").asLong())
    second
      .at("/clientId")
      .asText()
      .assert
      .isNotEqualTo(first.at("/clientId").asText())
    appInstallService.findAll(testData.otherOrganization.id).assert.hasSize(1)
    appInstallService.findAll(testData.organization.id).assert.hasSize(1)
  }

  @Test
  fun `an organization installing somebody else's app never sees its app-level credentials`() {
    register(testData.organization.id)

    userAccount = testData.otherOwner
    performAuthPost(installUrl(testData.otherOrganization.id), manifestBody()).andIsOk.andAssertThatJson {
      node("app.clientId").isNull()
      node("app.clientSecret").isNull()
      node("app.webhookSecret").isNull()
    }
  }

  /**
   * Registering an app somebody else already owns must not hand over its credentials either — the
   * install is created, the ownership is not.
   */
  @Test
  fun `registering an already-registered app only installs it`() {
    register(testData.organization.id)

    userAccount = testData.otherOwner
    performAuthPost(registerUrl(testData.otherOrganization.id), manifestBody()).andIsOk.andAssertThatJson {
      node("app.clientSecret").isNull()
    }

    ownerOrganizationIdOf("test-app").assert.isEqualTo(testData.organization.id)
  }

  @Test
  fun `a native app is owned by the server, not by an organization`() {
    userAccount = testData.admin

    performAuthPost("/v2/administration/apps", manifestBody()).andIsOk.andAssertThatJson {
      node("app.clientSecret").isString.startsWith(AppService.APP_CLIENT_SECRET_PREFIX)
    }

    ownerOrganizationIdOf("test-app").assert.isNull()
  }

  @Test
  fun `deregistering the last native install deregisters the app`() {
    userAccount = testData.admin
    val installId =
      objectMapper
        .readTree(
          performAuthPost("/v2/administration/apps", manifestBody())
            .andIsOk
            .andReturn()
            .response.contentAsString,
        ).at("/id")
        .asLong()

    performAuthDelete("/v2/administration/apps/$installId").andIsOk

    appRepository.findByAppId("test-app").assert.isNull()
  }

  private fun ownerOrganizationIdOf(appId: String): Long? =
    executeInNewTransaction(platformTransactionManager) {
      appRepository.findByAppId(appId)!!.organization?.id
    }

  private fun register(organizationId: Long) =
    objectMapper.readTree(
      performAuthPost(registerUrl(organizationId), manifestBody())
        .andIsOk
        .andReturn()
        .response.contentAsString,
    )

  private fun installUrl(organizationId: Long) = "/v2/organizations/$organizationId/apps"

  private fun registerUrl(organizationId: Long) = "${installUrl(organizationId)}/register"

  private fun manifestBody() = mapOf("manifestUrl" to AppsTestFixtures.MANIFEST_URL)

  companion object {
    private val MANIFEST_WITH_UNKNOWN_SCOPE =
      AppsTestFixtures.MANIFEST.replace(
        "\"modules\"",
        "\"scopes\": [\"not.a.real.scope\"],\n\"modules\"",
      )
  }
}
