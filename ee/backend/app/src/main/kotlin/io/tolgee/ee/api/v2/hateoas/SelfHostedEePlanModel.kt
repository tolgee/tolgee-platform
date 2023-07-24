package io.tolgee.ee.api.v2.hateoas

import io.tolgee.constants.Feature
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Suppress("unused")
@Relation(collectionRelation = "plans", itemRelation = "plan")
open class SelfHostedEePlanModel(
  val id: Long = 0,
  var name: String = "",
  val public: Boolean = true,
  val enabledFeatures: Array<Feature> = arrayOf(),
  val prices: PlanPricesModel,
  val includedUsage: PlanIncludedUsageModel = PlanIncludedUsageModel(),
  val hasYearlyPrice: Boolean = false,
) : RepresentationModel<SelfHostedEePlanModel>()
