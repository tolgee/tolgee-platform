package io.tolgee.api.v2.hateoas.organization

import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.api.v2.hateoas.permission.PermissionModel
import io.tolgee.dtos.Avatar
import io.tolgee.model.enums.OrganizationRoleType
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Relation(collectionRelation = "organizations", itemRelation = "organization")
open class OrganizationModel(
  val id: Long,

  @Schema(example = "Beautiful organization")
  val name: String,

  @Schema(example = "btforg")
  val slug: String,

  @Schema(example = "This is a beautiful organization full of beautiful and clever people")
  val description: String?,

  val basePermission: PermissionModel,

  @Schema(
    description = """The role of currently authorized user. 
    
Can be null when user has direct access to one of the projects owned by the organization."""
  )
  val currentUserRole: OrganizationRoleType?,

  @Schema(example = "Links to avatar images")
  var avatar: Avatar?
) : RepresentationModel<OrganizationModel>()
