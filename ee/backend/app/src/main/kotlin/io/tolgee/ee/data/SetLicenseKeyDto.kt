package io.tolgee.ee.data

import jakarta.validation.constraints.NotBlank

class SetLicenseKeyDto(
  @field:NotBlank
  var licenseKey: String = "",
)
