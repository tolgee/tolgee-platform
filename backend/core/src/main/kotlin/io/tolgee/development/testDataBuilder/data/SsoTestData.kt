package io.tolgee.development.testDataBuilder.data

import io.tolgee.model.SsoTenant
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.OrganizationRoleType

class SsoTestData : BaseTestData() {
  val organization = this.projectBuilder.self.organizationOwner

  var userAdmin: UserAccount
  var userNotOwner: UserAccount
  lateinit var tenant: SsoTenant

  init {
    userAdmin =
      root
        .addUserAccount {
          username = "userAdmin@unrelated.com"
          role = UserAccount.Role.ADMIN
        }.self
    userNotOwner =
      root
        .addUserAccount {
          username = "userNotOwner@domain.com"
        }.self
    userAccountBuilder.defaultOrganizationBuilder.apply {
      addRole {
        user = userNotOwner
        type = OrganizationRoleType.MEMBER
      }
    }
  }

  fun addTenant() {
    tenant =
      userAccountBuilder.defaultOrganizationBuilder
        .setTenant {
          domain = "domain.com"
          clientId = "dummy_client_id"
          clientSecret = "clientSecret"
          authorizationUri = "https://dummy-url.com"
          tokenUri = "http://tokenUri"
        }.self
  }
}
