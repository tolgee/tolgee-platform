package io.tolgee.hateoas.administration

import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.constants.Feature
import io.tolgee.constants.FeatureFlag
import org.springframework.hateoas.RepresentationModel

class OrganizationFeaturesModel(
  val featureFlags: List<FeatureFlagStateModel>,
  val planFeatures: List<PlanFeatureStateModel>,
  @get:Schema(
    description =
      "Whether plan features can be granted on this server. " +
        "Self-hosted instances get plan features only from their license.",
  )
  val planFeatureGrantsAllowed: Boolean,
) : RepresentationModel<OrganizationFeaturesModel>()

data class FeatureFlagStateModel(
  val flag: FeatureFlag,
  @get:Schema(description = "Whether the flag is enabled for this organization by a server administrator")
  val granted: Boolean,
  @get:Schema(description = "Whether the flag is enabled for all organizations by the server configuration")
  val enabledForAll: Boolean,
)

data class PlanFeatureStateModel(
  val feature: Feature,
  @get:Schema(description = "Whether the organization's plan or license includes the feature")
  val fromPlan: Boolean,
  @get:Schema(description = "Whether the feature is granted to the organization regardless of its plan")
  val granted: Boolean,
)
