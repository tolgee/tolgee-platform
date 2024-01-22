package io.tolgee.ee.data

import jakarta.validation.constraints.NotBlank

class ReleaseKeyDto(
  @field:NotBlank
  var licenseKey: String = "",
)
