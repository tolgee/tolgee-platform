package io.tolgee.model.views

import io.tolgee.model.Permission
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.OrganizationRoleType

interface ProjectView {
    val id: Long
    val name: String
    val description: String?
    val slug: String?
    val userOwner: UserAccount?
    val organizationOwnerName: String?
    val organizationOwnerSlug: String?
    val organizationBasePermissions: Permission.ProjectPermissionType?
    val organizationRole: OrganizationRoleType?
    val directPermissions: Permission.ProjectPermissionType?
}
