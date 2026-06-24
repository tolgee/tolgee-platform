package io.tolgee.batch.request

import io.tolgee.component.automations.processors.ContentDeliveryPublishWebhookData

data class WebhookDispatchBjRequest(
  var webhookConfigId: Long = 0,
  var projectId: Long = 0,
  var data: ContentDeliveryPublishWebhookData = ContentDeliveryPublishWebhookData(),
)
