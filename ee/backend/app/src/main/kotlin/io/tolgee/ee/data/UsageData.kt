package io.tolgee.ee.data

import java.math.BigDecimal

data class UsageData(
  val seatsUsage: List<ProportionalUsagePeriod>,
  val translationsUsage: List<ProportionalUsagePeriod>,
  val keysUsage: List<ProportionalUsagePeriod>,
  val creditsUsage: SumUsageItem?,
  val subscriptionPrice: BigDecimal?,
  val appliedStripeCredits: BigDecimal?,
) {
  val total: BigDecimal
    get() =
      seatsUsage.sumOf { it.total } +
        translationsUsage.sumOf { it.total } +
        keysUsage.sumOf { it.total } +
        (subscriptionPrice ?: 0.toBigDecimal()) +
        (creditsUsage?.total ?: 0.toBigDecimal())
}
