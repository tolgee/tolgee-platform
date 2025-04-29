package io.tolgee.hateoas.ee

import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation
import java.io.Serializable
import java.math.BigDecimal

@Suppress("unused")
@Relation(collectionRelation = "prices", itemRelation = "prices")
open class PlanPricesModel(
  val perSeat: BigDecimal = BigDecimal.ZERO,
  var perThousandTranslations: BigDecimal? = BigDecimal.ZERO,
  val perThousandMtCredits: BigDecimal? = BigDecimal.ZERO,
  val subscriptionMonthly: BigDecimal = BigDecimal.ZERO,
  val subscriptionYearly: BigDecimal = BigDecimal.ZERO,
  val perThousandKeys: BigDecimal = BigDecimal.ZERO,
) : RepresentationModel<PlanPricesModel>(), Serializable
