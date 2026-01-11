package io.tolgee.model.views

import io.tolgee.api.IMfa
import io.tolgee.model.enums.OrganizationRoleType

interface UserAccountWithOrganizationRoleView : IMfa {
  val id: Long
  val name: String
  var username: String
  var organizationRole: OrganizationRoleType
  override var totpKey: ByteArray?
  var avatarHash: String?
}
