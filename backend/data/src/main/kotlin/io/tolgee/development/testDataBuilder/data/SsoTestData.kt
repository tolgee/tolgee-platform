package io.tolgee.development.testDataBuilder.data

import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.model.Organization
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.OrganizationRoleType

class SsoTestData : BaseTestData() {
  val organization = this.projectBuilder.self.organizationOwner

  var userNotOwner: UserAccount
  var userNotOwnerOrganization: Organization
  val createUserNotOwner: TestDataBuilder =
    TestDataBuilder().apply {
      userNotOwner =
        addUserAccount userBuilder@{
          username = "userNotOwner"
        }.self
      userNotOwnerOrganization =
        addOrganization {
          name = "organization"
        }.build {
          addRole {
            user = userNotOwner
            type = OrganizationRoleType.MEMBER
          }
        }.self
    }
}
