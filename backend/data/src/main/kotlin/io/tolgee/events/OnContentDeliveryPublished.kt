package io.tolgee.events

import io.tolgee.component.automations.processors.ContentDeliveryPublishWebhookData
import org.springframework.context.ApplicationEvent

class OnContentDeliveryPublished(
  source: Any,
  val data: ContentDeliveryPublishWebhookData,
) : ApplicationEvent(source)
