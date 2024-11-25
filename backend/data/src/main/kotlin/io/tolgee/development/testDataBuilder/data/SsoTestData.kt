package io.tolgee.development.testDataBuilder.data

import io.tolgee.model.SsoTenant
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.OrganizationRoleType

class SsoTestData(createTenant: Boolean) : BaseTestData() {
  val organization = this.projectBuilder.self.organizationOwner

  var userNotOwner: UserAccount
  lateinit var tenant: SsoTenant

  init {
    root.apply {
      userNotOwner =
        addUserAccount userBuilder@{
          username = "userNotOwner"
        }.self
      userAccountBuilder.defaultOrganizationBuilder.apply {
        addRole {
          user = userNotOwner
          type = OrganizationRoleType.MEMBER
        }

        if (createTenant) {
          tenant =
            setTenant {
              domain = "registrationId"
              clientId = "dummy_client_id"
              clientSecret = "clientSecret"
              authorizationUri = "https://dummy-url.com"
              jwkSetUri = "http://jwkSetUri"
              tokenUri = "http://tokenUri"
            }.self
        }
      }
    }
  }
}
