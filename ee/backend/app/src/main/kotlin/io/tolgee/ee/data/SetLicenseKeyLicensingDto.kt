package io.tolgee.ee.data

import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.publicBilling.MetricType
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
  @field:Min(0)
  @Schema(
    description = "Number of words on the instance. If not provided, the number of words will not be updated.",
  )
  var words: Long? = null,
  @Schema(
    description =
      "Metrics this instance is able to measure and report. Absent on instances released before " +
        "a metric existed, which is how the server tells it cannot report that metric at all.",
  )
  var reportedMetrics: Set<MetricType>? = null,
)
