package io.tolgee.ee

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.constants.Feature
import io.tolgee.development.testDataBuilder.data.WebhooksTestData
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.fixtures.andIsOk
import io.tolgee.model.automations.AutomationTriggerType
import io.tolgee.service.automations.AutomationService
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(properties = ["tolgee.cache.enabled=true"])
class WebhookEventTypesCacheTest : ProjectAuthControllerTest("/v2/projects/") {
  @Autowired
  lateinit var automationService: AutomationService

  @Autowired
  lateinit var enabledFeaturesProvider: PublicEnabledFeaturesProvider

  lateinit var testData: WebhooksTestData

  @BeforeEach
  fun setup() {
    testData = WebhooksTestData()
    projectSupplier = { testData.projectBuilder.self }
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    enabledFeaturesProvider.forceEnabled = setOf(Feature.WEBHOOKS)
  }

  @AfterEach
  fun after() {
    enabledFeaturesProvider.forceEnabled = null
    testDataService.cleanTestData(testData.root)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `unsubscribing from project activity evicts cached activity automations`() {
    val projectId = testData.projectBuilder.self.id
    automationService.getProjectAutomations(projectId, AutomationTriggerType.ACTIVITY).assert.isNotEmpty

    performProjectAuthPut(
      "webhook-configs/${testData.webhookConfig.self.id}",
      mapOf("url" to testData.webhookConfig.self.url, "eventTypes" to listOf("CONTENT_DELIVERY_PUBLISH")),
    ).andIsOk

    automationService.getProjectAutomations(projectId, AutomationTriggerType.ACTIVITY).assert.isEmpty()
  }
}
