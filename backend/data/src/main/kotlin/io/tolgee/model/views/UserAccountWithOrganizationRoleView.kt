package io.tolgee.model.views

import io.tolgee.model.enums.OrganizationRoleType

interface UserAccountWithOrganizationRoleView {
  val id: Long
  val name: String
  var username: String
  var organizationRole: OrganizationRoleType
  var avatarHash: String?
}
