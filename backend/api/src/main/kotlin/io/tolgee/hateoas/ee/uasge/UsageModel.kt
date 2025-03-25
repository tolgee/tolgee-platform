package io.tolgee.hateoas.ee.uasge

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation
import java.io.Serializable
import java.math.BigDecimal

@Suppress("unused")
@Relation(collectionRelation = "invoices", itemRelation = "invoice")
open class UsageModel(
  val subscriptionPrice: BigDecimal? = 0.toBigDecimal(),
  @Schema(
    description =
      "Relevant for invoices only. When there are " +
        "applied stripe credits, we need to reduce the total price by this amount.",
  )
  val appliedStripeCredits: BigDecimal? = null,
  val seats: AverageProportionalUsageItemModel = AverageProportionalUsageItemModel(),
  val translations: AverageProportionalUsageItemModel = AverageProportionalUsageItemModel(),
  val credits: SumUsageItemModel?,
  val total: BigDecimal = 0.toBigDecimal(),
) : RepresentationModel<UsageModel>(), Serializable
