package io.tolgee.hateoas.ee.uasge.proportional

import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation
import java.io.Serializable
import java.math.BigDecimal

@Suppress("unused")
@Relation(collectionRelation = "invoices", itemRelation = "invoice")
open class ProportionalUsageItemModel(
  val from: Long = 0,
  val to: Long = 0,
  val milliseconds: Long = 0,
  val total: BigDecimal = 0.toBigDecimal(),
  val unusedQuantity: Long = 0,
  val usedQuantity: Long = 0,
  val usedQuantityOverPlan: Long = 0,
) : RepresentationModel<ProportionalUsageItemModel>(), Serializable
