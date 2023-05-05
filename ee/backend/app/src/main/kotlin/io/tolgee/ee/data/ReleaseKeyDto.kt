package io.tolgee.ee.data

import javax.validation.constraints.NotBlank

class ReleaseKeyDto(
  @field:NotBlank
  var licenseKey: String = "",
)
