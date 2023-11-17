package io.tolgee.model.automations

import io.tolgee.component.automations.AutomationProcessor
import io.tolgee.component.automations.processors.ContentDeliveryPublishProcessor
import io.tolgee.component.automations.processors.WebhookProcessor
import kotlin.reflect.KClass

enum class AutomationActionType(
  val processor: KClass<out AutomationProcessor>
) {
  CONTENT_DELIVERY_PUBLISH(ContentDeliveryPublishProcessor::class),
  WEBHOOK(WebhookProcessor::class)
}
