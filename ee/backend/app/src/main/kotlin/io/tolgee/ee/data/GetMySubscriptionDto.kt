package io.tolgee.ee.data

import jakarta.validation.constraints.NotBlank

class GetMySubscriptionDto(
  @field:NotBlank
  var licenseKey: String = "",
  @field:NotBlank
  var instanceId: String = "",
)
