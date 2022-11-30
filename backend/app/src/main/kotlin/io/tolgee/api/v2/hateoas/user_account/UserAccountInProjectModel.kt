package io.tolgee.api.v2.hateoas.user_account

import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.api.v2.hateoas.permission.PermissionModel
import io.tolgee.model.enums.OrganizationRoleType
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Relation(collectionRelation = "users", itemRelation = "user")
data class UserAccountInProjectModel(
  val id: Long,
  val username: String,
  var name: String?,
  val organizationRole: OrganizationRoleType?,
  val organizationBasePermission: PermissionModel,
  val directPermission: PermissionModel?,
  @Schema(
    description = "Actual user's permissions on selected project. You can not sort data by this column!",
    example = "EDIT"
  )
  val computedPermission: PermissionModel,
) : RepresentationModel<UserAccountInProjectModel>()
