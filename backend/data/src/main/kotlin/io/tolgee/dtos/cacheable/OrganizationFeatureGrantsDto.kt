package io.tolgee.dtos.cacheable

import io.tolgee.constants.Feature
import io.tolgee.constants.FeatureFlag

data class OrganizationFeatureGrantsDto(
  val featureFlags: Set<FeatureFlag>,
  val planFeatures: Set<Feature>,
)
