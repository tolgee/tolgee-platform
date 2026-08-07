package io.tolgee.hateoas.organization.apps

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Relation(collectionRelation = "organizations", itemRelation = "organization")
open class AppAvailableOrganizationModel(
  val id: Long,
  @Schema(example = "Beautiful organization")
  val name: String,
  @Schema(example = "btforg")
  val slug: String,
) : RepresentationModel<AppAvailableOrganizationModel>()
