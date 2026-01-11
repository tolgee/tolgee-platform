package io.tolgee.dtos.request

import jakarta.validation.constraints.Size

data class WebhookConfigRequest(
  @field:Size(max = 255)
  var url: String = "",
)
