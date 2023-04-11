package io.tolgee.ee.data

import javax.validation.constraints.NotBlank

class GetMySubscriptionDto(
  @field:NotBlank
  var licenseKey: String = ""
)
