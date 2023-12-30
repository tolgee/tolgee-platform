package io.tolgee.hateoas.organization

import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.dtos.Avatar
import io.tolgee.hateoas.permission.PermissionModel
import io.tolgee.model.enums.OrganizationRoleType

interface IOrganizationModel {
  val id: Long

  @get:Schema(example = "Beautiful organization")
  val name: String

  @get:Schema(example = "btforg")
  val slug: String

  @get:Schema(example = "This is a beautiful organization full of beautiful and clever people")
  val description: String?
  val basePermissions: PermissionModel

  @get:Schema(
    description = """The role of currently authorized user. 
    
Can be null when user has direct access to one of the projects owned by the organization.""",
  )
  val currentUserRole: OrganizationRoleType?

  @get:Schema(example = "Links to avatar images")
  var avatar: Avatar?
}
