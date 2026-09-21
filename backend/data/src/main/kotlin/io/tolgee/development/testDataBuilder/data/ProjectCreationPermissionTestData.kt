package io.tolgee.development.testDataBuilder.data

import io.tolgee.model.UserAccount
import io.tolgee.model.enums.OrganizationRoleType

class ProjectCreationPermissionTestData : BaseTestData("orgOwner", "Existing project") {
  val owner get() = user
  val member: UserAccount
  val nonMember: UserAccount
  val serverAdmin: UserAccount
  val serverSupporter: UserAccount

  init {
    member =
      root
        .addUserAccountWithoutOrganization {
          username = "plainMemberUser"
          name = "Plain Member User"
        }.self
    userAccountBuilder.defaultOrganizationBuilder.build {
      addRole {
        user = member
        type = OrganizationRoleType.MEMBER
      }
    }
    nonMember =
      root
        .addUserAccountWithoutOrganization {
          username = "nonMemberUser"
          name = "Non Member User"
        }.self
    serverAdmin =
      root
        .addUserAccountWithoutOrganization {
          username = "serverAdminUser"
          name = "Server Admin User"
          role = UserAccount.Role.ADMIN
        }.self
    serverSupporter =
      root
        .addUserAccountWithoutOrganization {
          username = "serverSupporterUser"
          name = "Server Supporter User"
          role = UserAccount.Role.SUPPORTER
        }.self
  }
}
