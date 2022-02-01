package io.tolgee.model.views

import io.tolgee.model.Organization
import io.tolgee.model.Permission
import io.tolgee.model.enums.OrganizationRoleType

interface OrganizationView {
  val id: Long
  val name: String
  val description: String?
  val slug: String
  val basePermissions: Permission.ProjectPermissionType
  val currentUserRole: OrganizationRoleType
  val avatarHash: String?

  companion object {
    fun of(entity: Organization, currentUserRole: OrganizationRoleType): OrganizationView {
      return object : OrganizationView {
        override val id = entity.id
        override val name = entity.name!!
        override val description = entity.description
        override val slug = entity.slug!!
        override val basePermissions = entity.basePermissions
        override val currentUserRole = currentUserRole
        override val avatarHash = entity.avatarHash
      }
    }
  }
}
