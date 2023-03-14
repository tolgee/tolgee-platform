package io.tolgee.model.views

import io.tolgee.model.Permission
import io.tolgee.model.enums.OrganizationRoleType

interface UserAccountInProjectView {
  val id: Long
  val name: String?
  val username: String
  val organizationRole: OrganizationRoleType?
  val directPermission: Permission?
  val avatarHash: String?
}
