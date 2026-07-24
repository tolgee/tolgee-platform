package io.tolgee.development.testDataBuilder.data

import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.OrganizationRoleType
import io.tolgee.model.enums.ProjectPermissionType
import java.util.Date

class ContributorsTestData {
  lateinit var project: Project
  lateinit var admin: UserAccount
  lateinit var contributor: UserAccount
  lateinit var contributor2: UserAccount
  lateinit var member: UserAccount
  lateinit var orgMember: UserAccount
  lateinit var deletedContributor: UserAccount
  lateinit var disabledContributor: UserAccount

  val root: TestDataBuilder =
    TestDataBuilder().apply {
      val adminBuilder = addUserAccount { username = "admin@contributors.com" }
      admin = adminBuilder.self

      contributor =
        addUserAccount {
          username = "contributor@contributors.com"
          name = "Cora Contributor"
        }.self

      contributor2 =
        addUserAccount {
          username = "contributor2@contributors.com"
          name = "Cody Contributor"
        }.self

      member = addUserAccount { username = "member@contributors.com" }.self

      orgMember = addUserAccount { username = "orgmember@contributors.com" }.self
      adminBuilder.defaultOrganizationBuilder.build {
        addRole {
          user = orgMember
          type = OrganizationRoleType.MEMBER
        }
      }

      deletedContributor =
        addUserAccount {
          username = "deleted@contributors.com"
          deletedAt = Date()
        }.self

      disabledContributor =
        addUserAccount {
          username = "disabled@contributors.com"
          disabledAt = Date()
        }.self

      project =
        addProject(organizationOwner = adminBuilder.defaultOrganizationBuilder.self) {
          name = "Contributors project"
        }.build {
          addPermission {
            user = member
            type = ProjectPermissionType.VIEW
          }
        }.self
    }
}
