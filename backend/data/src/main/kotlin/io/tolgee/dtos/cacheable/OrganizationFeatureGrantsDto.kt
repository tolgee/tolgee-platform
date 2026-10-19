package io.tolgee.dtos.cacheable

import io.tolgee.constants.Feature
import java.io.Serializable

data class OrganizationFeatureGrantsDto(
  val features: Set<Feature>,
) : Serializable
