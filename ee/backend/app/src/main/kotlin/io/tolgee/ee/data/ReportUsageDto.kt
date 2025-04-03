package io.tolgee.ee.data

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank

class ReportUsageDto(
  @field:NotBlank
  var licenseKey: String = "",
  @field:Min(0)
  @Schema(
    description = "Number of keys in the project. If not provided, the number of keys will not be updated.",
  )
  var keys: Long? = null,
  @field:Min(0)
  @Schema(
    description = "Number of languages in the project. If not provided, the number of languages will not be updated.",
  )
  var seats: Long? = null,
)
