package io.tolgee.dtos.request.organization

import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.constants.Feature
import io.tolgee.constants.FeatureFlag

data class SetOrganizationFeaturesRequest(
  @field:Schema(description = "Feature flags enabled for the organization. Replaces the current set.")
  val featureFlags: Set<FeatureFlag> = emptySet(),
  @field:Schema(
    description = "Plan features granted to the organization regardless of its plan. Replaces the current set.",
  )
  val grantedFeatures: Set<Feature> = emptySet(),
)
