package io.tolgee.development.testDataBuilder.data

import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.OrganizationRoleType

class BusinessEventTestData : BaseTestData() {
  val foreignOrganizationBuilder =
    root.addOrganization {
      name = "Business Event Foreign Org"
    }

  val memberOrganizationBuilder =
    root.addOrganization {
      name = "Business Event Member Org"
    }

  val softDeletedProject: Project

  val outsider: UserAccount
  val admin: UserAccount
  val supporter: UserAccount

  init {
    memberOrganizationBuilder.addRole {
      user = this@BusinessEventTestData.user
      type = OrganizationRoleType.MEMBER
    }

    softDeletedProject =
      root
        .addProject(organizationOwner = foreignOrganizationBuilder.self) {
          name = "Business Event Soft Deleted Project"
        }.build {
          setDeletedAt()
        }.self

    outsider = reporter("business_event_outsider")
    admin = reporter("business_event_admin", UserAccount.Role.ADMIN)
    supporter = reporter("business_event_supporter", UserAccount.Role.SUPPORTER)
  }

  private fun reporter(
    username: String,
    role: UserAccount.Role = UserAccount.Role.USER,
  ): UserAccount =
    root
      .addUserAccount {
        this.username = username
        this.role = role
      }.self
}
