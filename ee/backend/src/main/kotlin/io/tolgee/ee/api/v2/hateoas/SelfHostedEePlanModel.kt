package io.tolgee.ee.api.v2.hateoas

import io.tolgee.constants.Feature
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation
import java.math.BigDecimal

@Suppress("unused")
@Relation(collectionRelation = "plans", itemRelation = "plan")
class SelfHostedEePlanModel(
  val id: Long = 0,
  var name: String = "",
  val public: Boolean = true,
  val enabledFeatures: Array<Feature> = arrayOf(),
  val includedSeats: Long = 0L,
  val pricePerSeat: BigDecimal = BigDecimal(0),
) : RepresentationModel<SelfHostedEePlanModel>()
