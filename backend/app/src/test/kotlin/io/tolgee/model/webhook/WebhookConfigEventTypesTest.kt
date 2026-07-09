package io.tolgee.model.webhook

import io.tolgee.AbstractSpringTest
import io.tolgee.component.automations.processors.WebhookEventType
import io.tolgee.development.testDataBuilder.data.WebhooksTestData
import io.tolgee.repository.WebhookConfigRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class WebhookConfigEventTypesTest : AbstractSpringTest() {
  @Autowired
  lateinit var webhookConfigRepository: WebhookConfigRepository

  @Test
  fun `persists and reads back event types`() {
    val testData = WebhooksTestData()
    testData.webhookConfig.self.eventTypes =
      mutableSetOf(WebhookEventType.CONTENT_DELIVERY_PUBLISH)
    testDataService.saveTestData(testData.root)

    val reloaded = webhookConfigRepository.findById(testData.webhookConfig.self.id).get()
    assertThat(reloaded.eventTypes).containsExactly(WebhookEventType.CONTENT_DELIVERY_PUBLISH)
  }
}
