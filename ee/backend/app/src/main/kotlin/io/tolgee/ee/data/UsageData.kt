package io.tolgee.ee.data

import java.math.BigDecimal

data class UsageData(
  val usage: List<UsagePeriod>,
  val subscriptionPrice: BigDecimal
)
