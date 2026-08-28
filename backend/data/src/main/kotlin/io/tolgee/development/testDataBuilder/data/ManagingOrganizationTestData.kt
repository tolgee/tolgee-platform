package io.tolgee.development.testDataBuilder.data

import io.tolgee.model.Organization
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.OrganizationRoleType

class ManagingOrganizationTestData : BaseTestData() {
  val managedUser: UserAccount
  val managingOrganization: Organization

  init {
    val managedUserBuilder =
      root.addUserAccount {
        username = "managed_user@test.com"
      }
    managedUser = managedUserBuilder.self

    val organizationBuilder =
      root.addOrganization {
        name = "Managing org"
      }
    organizationBuilder.addRole {
      user = managedUser
      type = OrganizationRoleType.MEMBER
      managed = true
    }
    managingOrganization = organizationBuilder.self
  }
}
