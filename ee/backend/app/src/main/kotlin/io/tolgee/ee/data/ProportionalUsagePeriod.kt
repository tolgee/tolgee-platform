package io.tolgee.ee.data

import java.math.BigDecimal

data class ProportionalUsagePeriod(
  val from: Long,
  val to: Long,
  val milliseconds: Long,
  val total: BigDecimal,
  val usedQuantityOverPlan: Long,
  val unusedQuantity: Long,
  val usedQuantity: Long,
)
