package io.tolgee.hateoas.organization

import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.constants.Feature
import io.tolgee.hateoas.quickStart.QuickStartModel
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Suppress("unused")
@Relation(collectionRelation = "organizations", itemRelation = "organization")
open class PrivateOrganizationModel(
  organizationModel: OrganizationModel,

  @get:Schema(example = "Features organization has enabled")
  val enabledFeatures: Array<Feature>,

  @get:Schema(example = "Quick start data for current user")
  val quickStart: QuickStartModel?
) : IOrganizationModel by organizationModel, RepresentationModel<PrivateOrganizationModel>()
