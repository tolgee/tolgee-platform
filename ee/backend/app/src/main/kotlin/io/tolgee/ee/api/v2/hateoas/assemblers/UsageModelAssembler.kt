package io.tolgee.ee.api.v2.hateoas.assemblers

import io.tolgee.ee.data.ProportionalUsagePeriod
import io.tolgee.ee.data.SumUsageItem
import io.tolgee.ee.data.UsageData
import io.tolgee.hateoas.ee.uasge.proportional.AverageProportionalUsageItemModel
import io.tolgee.hateoas.ee.uasge.proportional.SumUsageItemModel
import io.tolgee.hateoas.ee.uasge.proportional.UsageModel
import org.springframework.hateoas.server.RepresentationModelAssembler
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.math.RoundingMode

@Component
class UsageModelAssembler : RepresentationModelAssembler<UsageData, UsageModel> {
  override fun toModel(data: UsageData): UsageModel {
    return UsageModel(
      subscriptionPrice = data.subscriptionPrice,
      seats = this.periodToModel(data.seatsUsage),
      translations = this.periodToModel(data.translationsUsage),
      keys = this.periodToModel(data.keysUsage),
      credits = data.creditsUsage?.let { sumToModel(it) },
      total = data.total,
      appliedStripeCredits = data.appliedStripeCredits,
    )
  }

  fun sumToModel(sum: SumUsageItem): SumUsageItemModel {
    return SumUsageItemModel(
      total = sum.total,
      unusedQuantity = sum.unusedQuantity,
      usedQuantity = sum.usedQuantity,
      usedQuantityOverPlan = sum.usedQuantityOverPlan,
    )
  }

  fun periodToModel(periods: List<ProportionalUsagePeriod>): AverageProportionalUsageItemModel {
    val total = periods.sumOf { it.total }

    return AverageProportionalUsageItemModel(
      total = total,
      unusedQuantity = periods.msWeightedAverageOf { it.unusedQuantity },
      usedQuantity = periods.msWeightedAverageOf { it.usedQuantity },
      usedQuantityOverPlan = periods.msWeightedAverageOf { it.usedQuantityOverPlan },
    )
  }

  fun List<ProportionalUsagePeriod>.msWeightedAverageOf(property: (ProportionalUsagePeriod) -> Long): BigDecimal {
    val sumMs = this.sumOf { it.milliseconds }
    if (sumMs == 0L) {
      return 0.toBigDecimal()
    }
    return this
      .sumOf { property(it) * it.milliseconds }
      .toBigDecimal()
      .divide(sumMs.toBigDecimal(), 2, RoundingMode.HALF_UP)
  }
}
