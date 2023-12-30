package io.tolgee.dtos

import io.tolgee.dtos.cacheable.PermissionDto
import io.tolgee.model.enums.OrganizationRoleType

data class ProjectPermissionData(
  val directPermissions: PermissionDto?,
  val organizationRole: OrganizationRoleType?,
  val organizationBasePermissions: PermissionDto?,
  val computedPermissions: ComputedPermissionDto,
)
