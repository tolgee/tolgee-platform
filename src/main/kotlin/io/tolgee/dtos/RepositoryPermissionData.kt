package io.tolgee.dtos

import io.tolgee.model.Organization
import io.tolgee.model.Permission
import io.tolgee.model.Repository
import io.tolgee.model.enums.OrganizationRoleType

data class RepositoryPermissionData(
        val repository: Repository,
        val organization: Organization?,
        val directPermissions: Permission?,
        val organizationRole: OrganizationRoleType?,
        val organizationBasePermissions: Permission.RepositoryPermissionType?,
        val computedPermissions: Permission.RepositoryPermissionType?
)
