package io.tolgee.api.v2.controllers.apps

import io.tolgee.development.testDataBuilder.data.AppsTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsNotFound
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.node
import io.tolgee.repository.apps.AppRepository
import io.tolgee.service.apps.AppManifestHttpClient
import io.tolgee.service.apps.AppSecretRotationService
import io.tolgee.service.apps.AppService
import io.tolgee.service.apps.AppsTestFixtures
import io.tolgee.service.apps.lifecycle.AppLifecycleHttpClient
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.assert
import io.tolgee.util.executeInNewTransaction
import net.javacrumbs.jsonunit.assertj.assertThatJson
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.ResultActions
import tools.jackson.databind.JsonNode

/**
 * The outbound lifecycle channel: registering an app and rolling either of its secrets hands the
 * app a signed POST, synchronously, and the triggering response says whether it landed. A dead app
 * host makes the delivery fail as a reported value, never undoing the change that produced the
 * credentials.
 */
class AppLifecycleDeliveryTest : AuthorizedControllerTest() {
  @Autowired
  lateinit var appRepository: AppRepository

  @Autowired
  lateinit var appService: AppService

  @Autowired
  lateinit var appSecretRotationService: AppSecretRotationService

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
    AppsTestFixtures.mockManifest(appManifestHttpClient)
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `registering an app delivers app_registered with the disclosed credentials`() {
    val registered =
      objectMapper.readTree(
        registerResult()
          .andAssertThatJson {
            node("delivery.attempted").isEqualTo(true)
            node("delivery.delivered").isEqualTo(true)
          }.andReturn()
          .response.contentAsString,
      )

    val delivered = capturedDeliveries().single { it.eventType == "app.registered" }
    assertThatJson(delivered.payload).apply {
      node("app.clientId").isEqualTo(registered.get("clientId").asText())
      node("app.clientSecret").isEqualTo(registered.get("clientSecret").asText())
      node("app.webhookSecret").isEqualTo(registered.get("webhookSecret").asText())
      node("organization.id").isEqualTo(testData.organization.id)
    }
  }

  @Test
  fun `a failed registration delivery is reported, not thrown, and the app still registers`() {
    failDelivery()

    val registered =
      objectMapper.readTree(
        registerResult()
          .andAssertThatJson {
            node("delivery.attempted").isEqualTo(true)
            node("delivery.delivered").isEqualTo(false)
            node("delivery.error").isString.isNotEmpty()
          }.andReturn()
          .response.contentAsString,
      )

    val appEntityId = registered.get("id").asLong()
    executeInNewTransaction(platformTransactionManager) {
      appRepository
        .findById(appEntityId)
        .orElse(null)
        .assert.isNotNull
    }
  }

  @Test
  fun `the owner reveals the current webhook secret`() {
    val appEntityId = register().get("id").asLong()
    val stored = webhookSecretOf(appEntityId)

    performAuthGet("${ownedUrl()}/$appEntityId/webhook-secret").andIsOk.andAssertThatJson {
      node("secret").isEqualTo(stored)
    }
  }

  @Test
  fun `rotating the webhook secret mints a new one and delivers it signed with the old secret`() {
    val registered = register()
    val appEntityId = registered.get("id").asLong()
    val oldSecret = registered.get("webhookSecret").asText()
    reset(appLifecycleHttpClient)

    val rotated =
      objectMapper.readTree(
        performAuthPost("${ownedUrl()}/$appEntityId/webhook-secret/rotate", null)
          .andIsOk
          .andAssertThatJson {
            node("secret").isString.isNotEqualTo(oldSecret)
            node("delivery.delivered").isEqualTo(true)
          }.andReturn()
          .response.contentAsString,
      )

    val newSecret = rotated.get("secret").asText()
    webhookSecretOf(appEntityId).assert.isEqualTo(newSecret)

    val call = capturedDeliveries().single { it.eventType == "app.secret_rotated" }
    assertThatJson(call.payload).apply {
      node("app.webhookSecret").isEqualTo(newSecret)
      node("app.clientSecret").isAbsent()
    }
    // Signed with the previous secret so a running app can verify the delivery that carries the new one.
    call.signingSecret.assert.isEqualTo(oldSecret)
  }

  @Test
  fun `rolling the client secret delivers app_secret_rotated with the new client secret`() {
    val appEntityId = register().get("id").asLong()
    reset(appLifecycleHttpClient)

    executeInNewTransaction(platformTransactionManager) {
      val app = appService.getRegistered(appEntityId)
      appSecretRotationService.rotate(app, ONE_DAY)
    }

    val call = capturedDeliveries().single { it.eventType == "app.secret_rotated" }
    assertThatJson(call.payload).apply {
      node("app.clientSecret").isString.startsWith(AppService.APP_CLIENT_SECRET_PREFIX)
      node("app.webhookSecret").isAbsent()
    }
  }

  @Test
  fun `an organization that does not own the app reaches none of its webhook secret`() {
    val appEntityId = register().get("id").asLong()

    userAccount = testData.otherOwner
    val otherOwned = "/v2/organizations/${testData.otherOrganization.id}/owned-apps"
    performAuthGet("$otherOwned/$appEntityId/webhook-secret").andIsNotFound
    performAuthPost("$otherOwned/$appEntityId/webhook-secret/rotate", null).andIsNotFound
  }

  private fun registerResult(): ResultActions {
    userAccount = testData.user
    return performAuthPost(ownedUrl(), mapOf("manifestUrl" to AppsTestFixtures.MANIFEST_URL)).andIsOk
  }

  private fun register(): JsonNode = objectMapper.readTree(registerResult().andReturn().response.contentAsString)

  private data class Delivery(
    val url: String,
    val eventType: String,
    val payload: String,
    val signingSecret: String,
  )

  private fun capturedDeliveries(): List<Delivery> {
    val urlCaptor = argumentCaptor<String>()
    val payloadCaptor = argumentCaptor<String>()
    val secretCaptor = argumentCaptor<String>()
    verify(
      appLifecycleHttpClient,
      atLeastOnce(),
    ).post(urlCaptor.capture(), payloadCaptor.capture(), secretCaptor.capture())
    return urlCaptor.allValues.indices.map {
      val payload = payloadCaptor.allValues[it]
      Delivery(
        url = urlCaptor.allValues[it],
        eventType = objectMapper.readTree(payload).get("eventType").asText(),
        payload = payload,
        signingSecret = secretCaptor.allValues[it],
      )
    }
  }

  private fun failDelivery() {
    doThrow(AppLifecycleHttpClient.DeliveryFailedException("app unreachable"))
      .whenever(appLifecycleHttpClient)
      .post(any(), any(), any())
  }

  private fun webhookSecretOf(appEntityId: Long): String =
    executeInNewTransaction(platformTransactionManager) {
      appRepository.findById(appEntityId).orElseThrow().webhookSecret
    }

  private fun ownedUrl() = "/v2/organizations/${testData.organization.id}/owned-apps"

  companion object {
    const val ONE_DAY = 86_400L
  }
}
