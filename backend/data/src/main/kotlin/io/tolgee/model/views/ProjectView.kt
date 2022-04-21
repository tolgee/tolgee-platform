package io.tolgee.model.views

import io.tolgee.model.Language
import io.tolgee.model.Organization
import io.tolgee.model.Permission
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.OrganizationRoleType

interface ProjectView {
  val id: Long
  val name: String
  val description: String?
  val slug: String?
  val avatarHash: String?
  val userOwner: UserAccount?
  val baseLanguage: Language?
  val organizationOwner: Organization?
  val organizationRole: OrganizationRoleType?
  val directPermissions: Permission.ProjectPermissionType?
}
