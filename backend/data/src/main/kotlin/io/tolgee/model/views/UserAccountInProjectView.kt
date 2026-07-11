package io.tolgee.model.views

import io.tolgee.api.IMfa
import io.tolgee.model.Permission
import io.tolgee.model.enums.OrganizationRoleType

interface UserAccountInProjectView : IMfa {
  val id: Long
  val name: String?
  val username: String
  val organizationRole: OrganizationRoleType?
  val directPermission: Permission?
  override val totpKey: ByteArray?
  val avatarHash: String?
}
