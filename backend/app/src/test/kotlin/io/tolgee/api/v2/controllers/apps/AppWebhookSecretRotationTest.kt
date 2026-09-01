package io.tolgee.api.v2.controllers.apps

import io.tolgee.development.testDataBuilder.data.NativeAppsTestData
import io.tolgee.fixtures.andIsNotFound
import io.tolgee.fixtures.andIsOk
import io.tolgee.repository.apps.AppRepository
import io.tolgee.service.apps.AppManifestHttpClient
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

class AppWebhookSecretRotationTest : AuthorizedControllerTest() {
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

  @BeforeEach
  fun setup() {
    testData = NativeAppsTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    AppsTestFixtures.mockManifest(appManifestHttpClient)
    appEntityId =
      objectMapper
        .readTree(
          performAuthPost(
            "/v2/organizations/${testData.organization.id}/apps/register",
            mapOf("manifestUrl" to AppsTestFixtures.MANIFEST_URL),
          ).andIsOk.andReturn().response.contentAsString,
        ).at("/app/id")
        .asLong()
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `rotates the webhook secret to a new value`() {
    val before = webhookSecretOf()

    val newSecret =
      objectMapper
        .readTree(
          performAuthPost("${ownedUrl()}/$appEntityId/webhook-secret", null)
            .andIsOk
            .andReturn()
            .response.contentAsString,
        ).get("secret")
        .asText()

    newSecret.assert.isNotEqualTo(before)
    webhookSecretOf().assert.isEqualTo(newSecret)
  }

  @Test
  fun `the owner reveals the current webhook secret`() {
    val stored = webhookSecretOf()

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
  fun `an organization that does not own the app cannot rotate its webhook secret`() {
    userAccount = testData.otherOwner
    performAuthPost(
      "/v2/organizations/${testData.otherOrganization.id}/owned-apps/$appEntityId/webhook-secret",
      null,
    ).andIsNotFound

    performAuthGet(
      "/v2/organizations/${testData.otherOrganization.id}/owned-apps/$appEntityId/webhook-secret",
    ).andIsNotFound
  }

  private fun ownedUrl() = "/v2/organizations/${testData.organization.id}/owned-apps"

  private fun webhookSecretOf(): String? =
    executeInNewTransaction(platformTransactionManager) {
      appRepository.findById(appEntityId).orElseThrow().webhookSecret
    }
}
