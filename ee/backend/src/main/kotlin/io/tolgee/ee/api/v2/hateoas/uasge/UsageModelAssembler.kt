package io.tolgee.ee.api.v2.hateoas.uasge

import io.tolgee.ee.data.UsageData
import org.springframework.hateoas.server.RepresentationModelAssembler
import org.springframework.stereotype.Component

@Component
class UsageModelAssembler : RepresentationModelAssembler<UsageData, UsageModel> {
  override fun toModel(data: UsageData): UsageModel {
    return UsageModel(
      subscriptionPrice = data.subscriptionPrice,
      periods = data.usage.map {
        UsageItemModel(
          from = it.from,
          to = it.to,
          milliseconds = it.milliseconds,
          total = it.total,
          unusedQuantity = it.unusedQuantity,
          usedQuantity = it.usedQuantity,
          usedQuantityOverPlan = it.usedQuantityOverPlan,
        )
      },
      total = data.usage.sumOf { it.total } + data.subscriptionPrice
    )
  }
}
