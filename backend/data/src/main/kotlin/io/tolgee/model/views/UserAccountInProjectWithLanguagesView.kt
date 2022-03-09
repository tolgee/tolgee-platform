package io.tolgee.model.views

import io.tolgee.model.Permission
import io.tolgee.model.enums.OrganizationRoleType

class UserAccountInProjectWithLanguagesView(
  override val id: Long,
  override val name: String?,
  override val username: String,
  override val organizationRole: OrganizationRoleType?,
  override val organizationBasePermissions: Permission.ProjectPermissionType?,
  override val directPermissions: Permission.ProjectPermissionType?,
  val permittedLanguageIds: List<Long>?
) : UserAccountInProjectView
