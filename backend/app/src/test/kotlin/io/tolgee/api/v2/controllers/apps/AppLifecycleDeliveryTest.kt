package io.tolgee.api.v2.controllers.apps

import io.tolgee.development.testDataBuilder.data.AppsTestData
import io.tolgee.fixtures.andIsNotFound
import io.tolgee.fixtures.andIsOk
import io.tolgee.repository.apps.AppRepository
import io.tolgee.service.apps.AppManifestHttpClient
import io.tolgee.service.apps.AppSecretRotationService
import io.tolgee.service.apps.AppService
import io.tolgee.service.apps.AppsTestFixtures
import io.tolgee.service.apps.lifecycle.AppLifecycleHttpClient
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.assert
import io.tolgee.util.executeInNewTransaction
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
    val registered = register()

    registered
      .at("/delivery/attempted")
      .asBoolean()
      .assert
      .isTrue()
    registered.at("/delivery/delivered").assert.isNotNull
    registered
      .get("delivery")
      .get("delivered")
      .asBoolean()
      .assert
      .isTrue()

    val payload = deliveredPayloads().single { it.get("eventType").asText() == "app.registered" }
    payload
      .at("/app/clientId")
      .asText()
      .assert
      .isEqualTo(registered.get("clientId").asText())
    payload
      .at("/app/clientSecret")
      .asText()
      .assert
      .isEqualTo(registered.get("clientSecret").asText())
    payload
      .at("/app/webhookSecret")
      .asText()
      .assert
      .isEqualTo(registered.get("webhookSecret").asText())
    payload
      .at("/organization/id")
      .asLong()
      .assert
      .isEqualTo(testData.organization.id)
  }

  @Test
  fun `a failed registration delivery is reported, not thrown, and the app still registers`() {
    failDelivery()

    val registered = register()

    registered
      .at("/delivery/attempted")
      .asBoolean()
      .assert
      .isTrue()
    registered
      .at("/delivery/delivered")
      .asBoolean()
      .assert
      .isFalse()
    registered
      .at("/delivery/error")
      .asText()
      .assert
      .isNotEmpty()

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

    val revealed =
      objectMapper
        .readTree(
          performAuthGet("${ownedUrl()}/$appEntityId/webhook-secret")
            .andIsOk
            .andReturn()
            .response.contentAsString,
        ).get("secret")
        .asText()

    revealed.assert.isEqualTo(stored)
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
          .andReturn()
          .response.contentAsString,
      )

    val newSecret = rotated.get("secret").asText()
    newSecret.assert.isNotEqualTo(oldSecret)
    rotated
      .at("/delivery/delivered")
      .asBoolean()
      .assert
      .isTrue()
    webhookSecretOf(appEntityId).assert.isEqualTo(newSecret)

    val call = capturedDeliveries().single { it.payload.get("eventType").asText() == "app.secret_rotated" }
    call.payload
      .at("/app/webhookSecret")
      .asText()
      .assert
      .isEqualTo(newSecret)
    call.payload
      .at("/app/clientSecret")
      .isMissingNode.assert
      .isTrue()
    // The rotation delivery is signed with the previous secret so a running app can verify it.
    call.signingSecret.assert.isEqualTo(oldSecret)
  }

  @Test
  fun `rolling the client secret delivers app_secret_rotated with the new client secret`() {
    val registered = register()
    val appEntityId = registered.get("id").asLong()
    reset(appLifecycleHttpClient)

    executeInNewTransaction(platformTransactionManager) {
      val app = appService.getRegistered(appEntityId)
      appSecretRotationService.rotate(app, ONE_DAY)
    }

    val call = capturedDeliveries().single { it.payload.get("eventType").asText() == "app.secret_rotated" }
    call.payload
      .at("/app/clientSecret")
      .asText()
      .assert
      .startsWith(AppService.APP_CLIENT_SECRET_PREFIX)
    call.payload
      .at("/app/webhookSecret")
      .isMissingNode.assert
      .isTrue()
  }

  @Test
  fun `an organization that does not own the app reaches none of its webhook secret`() {
    val appEntityId = register().get("id").asLong()

    userAccount = testData.otherOwner
    val otherOwned = "/v2/organizations/${testData.otherOrganization.id}/owned-apps"
    performAuthGet("$otherOwned/$appEntityId/webhook-secret").andIsNotFound
    performAuthPost("$otherOwned/$appEntityId/webhook-secret/rotate", null).andIsNotFound
  }

  private fun register(): JsonNode {
    userAccount = testData.user
    return objectMapper.readTree(
      performAuthPost(ownedUrl(), mapOf("manifestUrl" to AppsTestFixtures.MANIFEST_URL))
        .andIsOk
        .andReturn()
        .response.contentAsString,
    )
  }

  private data class Delivery(
    val url: String,
    val payload: JsonNode,
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
      Delivery(
        url = urlCaptor.allValues[it],
        payload = objectMapper.readTree(payloadCaptor.allValues[it]),
        signingSecret = secretCaptor.allValues[it],
      )
    }
  }

  private fun deliveredPayloads(): List<JsonNode> = capturedDeliveries().map { it.payload }

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
