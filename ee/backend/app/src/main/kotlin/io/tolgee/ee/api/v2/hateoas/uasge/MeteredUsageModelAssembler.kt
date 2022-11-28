package io.tolgee.ee.api.v2.hateoas.uasge

import io.tolgee.ee.data.ProportionalUsagePeriod
import io.tolgee.ee.data.SumUsageItem
import io.tolgee.ee.data.UsageData
import org.springframework.hateoas.server.RepresentationModelAssembler
import org.springframework.stereotype.Component

@Component
class MeteredUsageModelAssembler : RepresentationModelAssembler<UsageData, MeteredUsageModel> {
  override fun toModel(data: UsageData): MeteredUsageModel {
    return MeteredUsageModel(
      subscriptionPrice = data.subscriptionPrice,
      seatsPeriods = data.seatsUsage.map(this::periodToModel),
      translationsPeriods = data.translationsUsage.map(this::periodToModel),
      credits = data.creditsUsage?.let { sumToModel(it) },
      total = data.total
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

  fun periodToModel(period: ProportionalUsagePeriod): ProportionalUsageItemModel {
    return ProportionalUsageItemModel(
      from = period.from,
      to = period.to,
      milliseconds = period.milliseconds,
      total = period.total,
      unusedQuantity = period.unusedQuantity,
      usedQuantity = period.usedQuantity,
      usedQuantityOverPlan = period.usedQuantityOverPlan,
    )
  }
}
