package io.tolgee.hateoas.apps

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Schema(description = "Organization owning the project, so a multi-tenant app can partition its work")
@Relation(itemRelation = "organization")
open class AppSelfProjectOrganizationModel(
  val id: Long,
  val name: String,
  val slug: String,
) : RepresentationModel<AppSelfProjectOrganizationModel>()
