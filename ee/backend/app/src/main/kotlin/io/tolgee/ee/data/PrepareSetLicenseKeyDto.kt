package io.tolgee.ee.data

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank

class PrepareSetLicenseKeyDto(
  @field:NotBlank
  var licenseKey: String = "",
  @field:Min(1)
  var seats: Long = 0,
)
