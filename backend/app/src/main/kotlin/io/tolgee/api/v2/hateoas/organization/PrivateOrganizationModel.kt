package io.tolgee.api.v2.hateoas.organization

import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.constants.Feature
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Suppress("unused")
@Relation(collectionRelation = "organizations", itemRelation = "organization")
open class PrivateOrganizationModel(
  organizationModel: OrganizationModel,

  @get:Schema(example = "Features organization has enabled")
  val enabledFeatures: Array<Feature>
) : IOrganizationModel by organizationModel, RepresentationModel<PrivateOrganizationModel>()
