package io.tolgee.ee.data

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank

class SetLicenseKeyLicensingDto(
  @field:NotBlank
  var licenseKey: String = "",
  @field:Min(1)
  var seats: Long = 0,
  @Schema(description = "Number of keys in the project. If not provided, the number of keys will not be updated.")
  @field:Min(0)
  var keys: Long? = null,
  @field:NotBlank
  var instanceId: String = "",
)
