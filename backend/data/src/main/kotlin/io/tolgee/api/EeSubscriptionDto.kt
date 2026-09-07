package io.tolgee.api

import io.tolgee.constants.Feature
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import org.hibernate.annotations.ColumnDefault
import java.util.Date

data class EeSubscriptionDto(
  @field:NotBlank
  var licenseKey: String,
  @field:ColumnDefault("Plan")
  var name: String,
  @field:NotNull
  var currentPeriodEnd: Date? = null,
  var cancelAtPeriodEnd: Boolean = false,
  var enabledFeatures: Array<Feature>,
  var status: SubscriptionStatus = SubscriptionStatus.ACTIVE,
  var lastValidCheck: Date? = null,
  var nonCommercial: Boolean = false,
  var includedKeys: Long,
  var includedSeats: Long,
  var isPayAsYouGo: Boolean,
  var keysLimit: Long,
  var seatsLimit: Long,
  var includedWords: Long = 0L,
  var wordsLimit: Long = -1L,
  var autoUpgradeEffective: Boolean = false,
  /**
   * Carried as a flag rather than the plan's MetricType, which lives in a module this one cannot
   * see. Metering is the only thing the self-hosted side asks the metric about.
   */
  var metersWords: Boolean = false,
)
