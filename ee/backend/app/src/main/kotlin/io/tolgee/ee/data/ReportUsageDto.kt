package io.tolgee.ee.data

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank

class ReportUsageDto(
  @field:NotBlank
  var licenseKey: String = "",
  @field:Min(0)
  var seats: Long = -1,
)
