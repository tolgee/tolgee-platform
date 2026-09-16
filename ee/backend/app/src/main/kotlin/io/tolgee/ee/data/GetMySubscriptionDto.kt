package io.tolgee.ee.data

import io.tolgee.publicBilling.MetricType
import jakarta.validation.constraints.NotBlank

class GetMySubscriptionDto(
  @field:NotBlank
  var licenseKey: String = "",
  @field:NotBlank
  var instanceId: String = "",
  /**
   * Re-declared on every refresh, not only at activation: a subscription can be moved onto a
   * metric its instance is too old to understand, and activation is the one moment that cannot
   * see it happen. Null from an instance that predates the field.
   */
  var reportedMetrics: Set<MetricType>? = null,
)
