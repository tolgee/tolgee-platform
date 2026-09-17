package io.tolgee.development.testDataBuilder.data

import io.tolgee.model.UserAccount

class ServerAdminProjectCreationTestData : BaseTestData("outsiderOrgOwner", "Outsider project") {
  val serverAdmin: UserAccount

  init {
    serverAdmin =
      root
        .addUserAccountWithoutOrganization {
          username = "serverAdminUser"
          name = "Server Admin User"
          role = UserAccount.Role.ADMIN
        }.self
  }
}
