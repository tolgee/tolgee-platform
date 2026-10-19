package io.tolgee.dtos.request.organization

import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.constants.Feature

data class SetOrganizationFeaturesRequest(
  @field:Schema(
    description = "Plan features granted to the organization regardless of its plan. Replaces the current set.",
  )
  val grantedFeatures: Set<Feature> = emptySet(),
)
