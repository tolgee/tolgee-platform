package io.tolgee.hateoas.administration

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.hateoas.RepresentationModel

class OrganizationFeaturesModel(
  val planFeatures: List<PlanFeatureStateModel>,
  @get:Schema(
    description =
      "Whether plan features can be granted on this server. " +
        "Self-hosted instances get plan features only from their license.",
  )
  val planFeatureGrantsAllowed: Boolean,
) : RepresentationModel<OrganizationFeaturesModel>()
