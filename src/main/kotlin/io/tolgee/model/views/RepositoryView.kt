package io.tolgee.model.views

import io.tolgee.model.Permission
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.OrganizationRoleType

interface RepositoryView {
    val id: Long
    val name: String
    val description: String?
    val addressPart: String?
    val userOwner: UserAccount?
    val organizationOwnerName: String?
    val organizationOwnerAddressPart: String?
    val organizationBasePermissions: Permission.RepositoryPermissionType?
    val organizationRole: OrganizationRoleType?
    val directPermissions: Permission.RepositoryPermissionType?
}
