package io.tolgee.hateoas.ee

import com.fasterxml.jackson.annotation.JsonGetter
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonSetter
import io.tolgee.constants.Feature
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Suppress("unused")
@Relation(collectionRelation = "plans", itemRelation = "plan")
open class SelfHostedEePlanModel(
  val id: Long = 0,
  var name: String = "",
  val public: Boolean = true,
  @JsonIgnore
  var enabledFeatures: Array<Feature> = arrayOf(),
  val prices: PlanPricesModel,
  val includedUsage: PlanIncludedUsageModel = PlanIncludedUsageModel(),
  val hasYearlyPrice: Boolean = false,
  val free: Boolean,
  val nonCommercial: Boolean,
  val isPayAsYouGo: Boolean,
) : RepresentationModel<SelfHostedEePlanModel>() {
  /**
   * We need to provide this setter so unrecognized features are ignored in situation
   * that self-hosted instance is not upgraded to version containing the new features introduced
   * in Tolgee cloud.
   */
  @JsonSetter("enabledFeatures")
  fun setJsonEnabledFeatures(features: Set<String>) {
    this.enabledFeatures = features.mapNotNull { Feature.findByName(it) }.toTypedArray()
  }

  @JsonGetter("enabledFeatures")
  fun getJsonEnabledFeatures(): Array<Feature> {
    return this.enabledFeatures
  }
}
