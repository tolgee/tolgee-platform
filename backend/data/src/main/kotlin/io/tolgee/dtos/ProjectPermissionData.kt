package io.tolgee.dtos

import io.tolgee.dtos.cacheable.PermissionDto
import io.tolgee.dtos.cacheable.ProjectDto
import io.tolgee.model.Permission
import io.tolgee.model.enums.OrganizationRoleType

data class ProjectPermissionData(
  val project: ProjectDto,
  val directPermissions: PermissionDto?,
  val organizationRole: OrganizationRoleType?,
  val organizationBasePermissions: Permission.ProjectPermissionType?,
  val computedPermissions: ComputedPermissionDto
)
