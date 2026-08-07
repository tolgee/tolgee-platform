package io.tolgee.api.v2.controllers.apps

import io.tolgee.development.testDataBuilder.data.NativeAppsTestData
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.service.apps.AppInstallService
import io.tolgee.service.apps.AppManifestHttpClient
import io.tolgee.service.apps.AppsTestFixtures
import io.tolgee.service.apps.lifecycle.AppLifecycleDeliveryDispatcher
import io.tolgee.service.apps.lifecycle.AppLifecycleDeliveryService
import io.tolgee.service.apps.lifecycle.AppLifecycleHttpClient
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.atLeast
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.clearInvocations
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.bean.override.mockito.MockitoBean
import tools.jackson.databind.JsonNode
import java.time.Duration

/**
 * The channel a published app learns everything over. Its two hard requirements are that credentials
 * reach the manifest's `baseUrl` and nowhere else, and that a failing delivery never takes down the
 * registration or install that triggered it.
 *
 * Deliveries are sent concurrently, so nothing here asserts the order they arrive in — only the
 * rows recording them are written in order.
 */
class AppLifecycleDeliveryTest : AuthorizedControllerTest() {
  @Autowired
  lateinit var appInstallService: AppInstallService

  @Autowired
  lateinit var appLifecycleDeliveryService: AppLifecycleDeliveryService

  @Autowired
  lateinit var appLifecycleDeliveryDispatcher: AppLifecycleDeliveryDispatcher

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
    clearInvocations(appLifecycleHttpClient)
  }

  @AfterEach
  fun cleanup() {
    clearForcedDate()
    AppsTestFixtures.removeNativeInstalls(appInstallService)
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `registering announces the app and the install to the manifest base url, signed with the app secret`() {
    val response = register()
    val webhookSecret = response.at("/app/webhookSecret").asText()

    val registered = awaitPayload("app.registered")
    val installed = awaitPayload("app.installed")

    registered
      .at("/app/clientId")
      .asText()
      .assert
      .isEqualTo(response.at("/app/clientId").asText())
    registered
      .at("/app/clientSecret")
      .asText()
      .assert
      .isEqualTo(response.at("/app/clientSecret").asText())
    registered
      .at("/app/webhookSecret")
      .asText()
      .assert
      .isEqualTo(webhookSecret)

    installed
      .at("/install/clientSecret")
      .isMissingNode.assert
      .isTrue()
    installed
      .at("/install/id")
      .asLong()
      .assert
      .isEqualTo(response.get("id").asLong())
    installed
      .at("/organization/id")
      .asLong()
      .assert
      .isEqualTo(testData.organization.id)
    installed
      .at("/app")
      .isMissingNode.assert
      .isTrue()

    val urls = argumentCaptor<String>()
    val secrets = argumentCaptor<String>()
    verify(appLifecycleHttpClient, atLeast(2)).post(urls.capture(), any(), secrets.capture())
    urls.allValues.assert.containsOnly(BASE_URL)
    secrets.allValues.assert.containsOnly(webhookSecret)
  }

  @Test
  fun `the install still succeeds when the app's host is down, and the failure is recorded`() {
    failDeliveries()

    register()

    appInstallService.findAll(testData.organization.id).assert.hasSize(1)
    waitForNotThrowing(throwableClass = AssertionError::class, timeout = 10000) {
      executeInNewTransaction {
        val deliveries = appLifecycleDeliveryService.listForApp(APP_ID)
        deliveries.assert.hasSize(2)
        deliveries.forEach {
          it.lastError.assert.isEqualTo("host is down")
          it.deliveredAt.assert.isNull()
          it.abandonedAt.assert.isNull()
        }
      }
    }
  }

  @Test
  fun `retries a failed delivery once the backoff has elapsed`() {
    failDeliveries()
    register()
    awaitAttempt()

    doNothing().whenever(appLifecycleHttpClient).post(any(), any(), any())
    moveCurrentDate(Duration.ofMinutes(10))
    appLifecycleDeliveryDispatcher.retryDue()

    executeInNewTransaction {
      appLifecycleDeliveryService.listForApp(APP_ID).forEach {
        it.deliveredAt.assert.isNotNull
        it.lastError.assert.isNull()
        it.attempts.assert.isGreaterThan(1)
      }
    }
  }

  @Test
  fun `uninstalling announces itself with the organization it concerns`() {
    val installId = register().get("id").asLong()
    awaitPayload("app.installed")

    performAuthDelete("${orgAppsUrl()}/$installId").andIsOk

    val uninstalled = awaitPayload("app.uninstalled")
    uninstalled
      .at("/organization/id")
      .asLong()
      .assert
      .isEqualTo(testData.organization.id)
    uninstalled
      .at("/install/id")
      .asLong()
      .assert
      .isEqualTo(installId)
    uninstalled
      .at("/install/clientSecret")
      .isMissingNode.assert
      .isTrue()
  }

  private fun failDeliveries() {
    doAnswer { throw AppLifecycleHttpClient.DeliveryFailedException("host is down") }
      .whenever(appLifecycleHttpClient)
      .post(any(), any(), any())
  }

  private fun awaitAttempt() {
    waitForNotThrowing(throwableClass = AssertionError::class, timeout = 10000) {
      executeInNewTransaction {
        appLifecycleDeliveryService.listForApp(APP_ID).forEach { it.attempts.assert.isGreaterThan(0) }
      }
    }
  }

  private fun awaitPayload(eventType: String): JsonNode {
    lateinit var found: JsonNode
    waitForNotThrowing(throwableClass = AssertionError::class, timeout = 10000) {
      val captor = argumentCaptor<String>()
      verify(appLifecycleHttpClient, atLeastOnce()).post(any(), captor.capture(), any())
      val seen = captor.allValues.map { objectMapper.readTree(it) }
      found =
        seen.lastOrNull { it.get("eventType").asText() == eventType }
          ?: throw AssertionError("no $eventType delivery yet, saw ${seen.map { it.get("eventType") }}")
    }
    return found
  }

  private fun register(): JsonNode {
    userAccount = testData.user
    return objectMapper.readTree(
      performAuthPost("${orgAppsUrl()}/register", mapOf("manifestUrl" to AppsTestFixtures.MANIFEST_URL))
        .andIsOk
        .andReturn()
        .response.contentAsString,
    )
  }

  private fun orgAppsUrl() = "/v2/organizations/${testData.organization.id}/apps"

  companion object {
    private const val APP_ID = "test-app"
    private const val BASE_URL = "https://app.example.com"
  }
}
