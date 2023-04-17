package io.tolgee.ee.data

import java.math.BigDecimal

data class UsageData(
  val seatsUsage: List<ProportionalUsagePeriod>,
  val translationsUsage: List<ProportionalUsagePeriod>,
  val creditsUsage: SumUsageItem?,
  val subscriptionPrice: BigDecimal?
)
