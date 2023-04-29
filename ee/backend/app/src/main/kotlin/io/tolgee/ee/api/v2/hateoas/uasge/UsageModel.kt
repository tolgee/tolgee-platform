package io.tolgee.ee.api.v2.hateoas.uasge

import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation
import java.io.Serializable
import java.math.BigDecimal

@Suppress("unused")
@Relation(collectionRelation = "invoices", itemRelation = "invoice")
open class UsageModel(
  val subscriptionPrice: BigDecimal? = 0.toBigDecimal(),
  val seats: AverageProportionalUsageItemModel = AverageProportionalUsageItemModel(),
  val translations: AverageProportionalUsageItemModel = AverageProportionalUsageItemModel(),
  val credits: SumUsageItemModel?,
  val total: BigDecimal = 0.toBigDecimal(),
) : RepresentationModel<UsageModel>(), Serializable
