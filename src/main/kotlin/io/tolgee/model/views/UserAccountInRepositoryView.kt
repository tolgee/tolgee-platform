package io.tolgee.model.views

import io.tolgee.model.Permission
import io.tolgee.model.enums.OrganizationRoleType

interface UserAccountInRepositoryView {
    val id: Long
    val name: String
    val username: String
    val organizationRole: OrganizationRoleType?
    val organizationBasePermissions: Permission.RepositoryPermissionType?
    val directPermissions: Permission.RepositoryPermissionType?
}
