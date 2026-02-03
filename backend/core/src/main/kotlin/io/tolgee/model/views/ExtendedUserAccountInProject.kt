package io.tolgee.model.views

import io.tolgee.model.Permission
import io.tolgee.model.enums.OrganizationRoleType

class ExtendedUserAccountInProject(
  val id: Long,
  val name: String?,
  val username: String,
  val organizationRole: OrganizationRoleType?,
  val organizationBasePermission: Permission,
  val directPermission: Permission?,
  val permittedLanguageIds: List<Long>?,
  val mfaEnabled: Boolean,
  val avatarHash: String?,
)
