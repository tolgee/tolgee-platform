package io.tolgee.model.batch.params

import io.tolgee.component.automations.processors.ContentDeliveryPublishWebhookData

class WebhookDispatchJobParams {
  var projectId: Long = 0
  var data: ContentDeliveryPublishWebhookData = ContentDeliveryPublishWebhookData()
}
