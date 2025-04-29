package io.tolgee.hateoas.ee.uasge.proportional

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.hateoas.RepresentationModel
import java.io.Serializable
import java.math.BigDecimal

/**
 * Presents usage to show usage for invoices or to show current expected usage.
 *
 * Is suitable for cases, where proportional usage is required.
 */
@Suppress("unused")
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
  val keys: AverageProportionalUsageItemModel = AverageProportionalUsageItemModel(),
  val total: BigDecimal = 0.toBigDecimal(),
) : RepresentationModel<UsageModel>(), Serializable
