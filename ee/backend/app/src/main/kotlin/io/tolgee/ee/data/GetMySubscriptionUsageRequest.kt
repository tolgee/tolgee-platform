package io.tolgee.ee.data

import jakarta.validation.constraints.NotBlank

class GetMySubscriptionUsageRequest(
  @field:NotBlank
  var licenseKey: String,
)
