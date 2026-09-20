package io.tolgee.component.automations

import io.tolgee.component.automations.processors.ContentDeliveryPublishWebhookData

data class AutomationTriggerContext(
  val activityRevisionId: Long? = null,
  val contentDeliveryPublish: ContentDeliveryPublishWebhookData? = null,
)
