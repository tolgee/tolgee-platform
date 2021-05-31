package io.tolgee.dtos

import io.tolgee.model.Organization
import io.tolgee.model.Permission
import io.tolgee.model.Project
import io.tolgee.model.enums.OrganizationRoleType

data class RepositoryPermissionData(
        val project: Project,
        val organization: Organization?,
        val directPermissions: Permission?,
        val organizationRole: OrganizationRoleType?,
        val organizationBasePermissions: Permission.ProjectPermissionType?,
        val computedPermissions: Permission.ProjectPermissionType?
)
