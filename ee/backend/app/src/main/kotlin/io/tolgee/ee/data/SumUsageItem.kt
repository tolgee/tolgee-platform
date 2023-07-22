package io.tolgee.ee.data

import java.math.BigDecimal

data class SumUsageItem(
  var total: BigDecimal,
  var usedQuantityOverPlan: Long,
  var unusedQuantity: Long,
  var usedQuantity: Long
)
