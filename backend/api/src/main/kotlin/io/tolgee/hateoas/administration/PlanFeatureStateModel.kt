package io.tolgee.hateoas.administration

import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.constants.Feature

data class PlanFeatureStateModel(
  val feature: Feature,
  @get:Schema(description = "Whether the organization's plan or license includes the feature")
  val fromPlan: Boolean,
  @get:Schema(description = "Whether the feature is granted to the organization regardless of its plan")
  val granted: Boolean,
  @get:Schema(description = "Whether the feature is still in development and invisible to users")
  val hidden: Boolean,
)
