package io.tolgee.batch.request

import io.tolgee.component.automations.processors.ContentDeliveryPublishWebhookData

data class AutomationBjRequest(
  var triggerId: Long,
  var actionId: Long,
  var activityRevisionId: Long?,
  var contentDeliveryPublish: ContentDeliveryPublishWebhookData? = null,
)
