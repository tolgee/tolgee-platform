package io.tolgee.development.testDataBuilder.data

import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.OrganizationRoleType
import io.tolgee.model.enums.ProjectPermissionType
import io.tolgee.model.enums.Scope
import java.util.Date

class ContributorsTestData {
  lateinit var project: Project
  lateinit var publicProject: Project
  lateinit var publicEmptyProject: Project
  lateinit var admin: UserAccount
  lateinit var contributor: UserAccount
  lateinit var contributor2: UserAccount
  lateinit var member: UserAccount
  lateinit var noneMember: UserAccount
  lateinit var orgMember: UserAccount
  lateinit var deletedContributor: UserAccount
  lateinit var disabledContributor: UserAccount
  lateinit var staffContributor: UserAccount
  lateinit var unnamedContributor: UserAccount
  lateinit var membersViewer: UserAccount
  lateinit var foreignOrgContributor: UserAccount

  val root: TestDataBuilder =
    TestDataBuilder().apply {
      val adminBuilder = addUserAccount { username = "admin@contributors.com" }
      admin = adminBuilder.self

      contributor =
        addUserAccount {
          username = "contributor@contributors.com"
          name = "Cora Contributor"
          avatarHash = "cora-avatar-hash"
        }.self

      contributor2 =
        addUserAccount {
          username = "contributor2@contributors.com"
          name = "Cody Contributor"
        }.self

      member = addUserAccount { username = "member@contributors.com" }.self

      noneMember = addUserAccount { username = "none@contributors.com" }.self

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

      staffContributor =
        addUserAccount {
          username = "staff@contributors.com"
          name = "Sam Staff"
          role = UserAccount.Role.ADMIN
        }.self

      unnamedContributor = addUserAccount { username = "unnamed@contributors.com" }.self

      membersViewer = addUserAccount { username = "viewer@contributors.com" }.self

      foreignOrgContributor = addUserAccount { username = "foreign@contributors.com" }.self
      addOrganization {
        name = "Foreign org"
      }.build {
        addRole {
          user = foreignOrgContributor
          type = OrganizationRoleType.MEMBER
        }
      }

      project =
        addProject(organizationOwner = adminBuilder.defaultOrganizationBuilder.self) {
          name = "Contributors project"
        }.build {
          addPermission {
            user = member
            type = ProjectPermissionType.VIEW
          }
          addPermission {
            user = noneMember
            type = ProjectPermissionType.NONE
          }
          addPermission {
            user = membersViewer
            scopes = arrayOf(Scope.MEMBERS_VIEW)
          }
        }.self

      publicProject =
        addProject(organizationOwner = adminBuilder.defaultOrganizationBuilder.self) {
          name = "Contributors public project"
          public = true
        }.self

      publicEmptyProject =
        addProject(organizationOwner = adminBuilder.defaultOrganizationBuilder.self) {
          name = "Contributors public empty project"
          public = true
        }.self
    }
}
