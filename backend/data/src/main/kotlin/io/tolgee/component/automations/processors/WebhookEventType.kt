package io.tolgee.component.automations.processors

import io.tolgee.model.automations.AutomationTriggerType

enum class WebhookEventType(
  val triggerType: AutomationTriggerType?,
) {
  TEST(null),
  PROJECT_ACTIVITY(AutomationTriggerType.ACTIVITY),
  CONTENT_DELIVERY_PUBLISH(AutomationTriggerType.CONTENT_DELIVERY_PUBLISH),
  ;

  companion object {
    fun fromTriggerType(triggerType: AutomationTriggerType): WebhookEventType? =
      entries.firstOrNull { it.triggerType == triggerType }
  }
}
