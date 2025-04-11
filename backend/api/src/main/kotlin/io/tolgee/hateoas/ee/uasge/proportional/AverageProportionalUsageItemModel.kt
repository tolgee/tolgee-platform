package io.tolgee.hateoas.ee.uasge.proportional

import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation
import java.io.Serializable
import java.math.BigDecimal

@Suppress("unused")
@Relation(collectionRelation = "invoices", itemRelation = "invoice")
open class AverageProportionalUsageItemModel(
  val total: BigDecimal = 0.toBigDecimal(),
  val unusedQuantity: BigDecimal = 0.toBigDecimal(),
  val usedQuantity: BigDecimal = 0.toBigDecimal(),
  val usedQuantityOverPlan: BigDecimal = 0.toBigDecimal(),
) : RepresentationModel<AverageProportionalUsageItemModel>(), Serializable
