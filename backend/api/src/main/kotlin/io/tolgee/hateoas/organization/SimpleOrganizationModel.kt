package io.tolgee.hateoas.organization

import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.dtos.Avatar
import io.tolgee.hateoas.permission.PermissionModel
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Relation(collectionRelation = "organizations", itemRelation = "organization")
open class SimpleOrganizationModel(
  val id: Long,
  @Schema(example = "Beautiful organization")
  val name: String,
  @Schema(example = "btforg")
  val slug: String,
  @Schema(example = "This is a beautiful organization full of beautiful and clever people")
  val description: String?,
  val basePermissions: PermissionModel,
  @Schema(example = "Links to avatar images")
  var avatar: Avatar?,
) : RepresentationModel<SimpleOrganizationModel>()
