package io.tolgee.dtos.request

import io.tolgee.component.automations.processors.WebhookEventType
import jakarta.validation.constraints.Size

data class WebhookConfigRequest(
  @field:Size(max = 255)
  var url: String = "",
  var enabled: Boolean? = null,
  @field:Size(min = 1, message = "At least one event type must be selected")
  var eventTypes: Set<WebhookEventType>? = null,
)
