package io.tolgee.ee.data

import javax.validation.constraints.NotBlank

class SetLicenseKeyDto(
  @field:NotBlank
  var licenseKey: String = "",
)
