package io.tolgee.development.testDataBuilder.data

import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.model.Organization
import io.tolgee.model.Permission
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.OrganizationRoleType

class ProjectTransferringTestData {
  lateinit var user2: UserAccount
  lateinit var user: UserAccount
  lateinit var organization: Organization
  lateinit var notOwnedOrganization: Organization
  lateinit var project: Project
  lateinit var vobtahlo: UserAccount

  val root = TestDataBuilder()

  init {
    root.apply {
      addUserAccount {
        username = "pepik"
        name = "Josef Kajetan"
        user2 = this
      }
      addUserAccount {
        username = "filip"
        name = "Filip Malecek"
        user = this
      }

      addOrganization {
        name = "Not owned organization"
        notOwnedOrganization = this
        slug = "not-owned-organization"
      }

      addOrganization {
        name = "Another organization"
        slug = "another-organization"
      }.build {
        addRole {
          user = this@ProjectTransferringTestData.user
          type = OrganizationRoleType.OWNER
        }
      }

      addOrganization {
        name = "Owned organization"
        organization = this
        slug = "owned-organization"
      }.build organizationBuilder@{
        addRole {
          type = OrganizationRoleType.OWNER
          user = this@ProjectTransferringTestData.user
        }
        addProject(organizationOwner = this@organizationBuilder.self) {
          name = "Organization owned project"
          organizationOwner = this@organizationBuilder.self
          project = this
        }.build {
          addPermission {
            user = this@ProjectTransferringTestData.user2
            type = Permission.ProjectPermissionType.VIEW
          }
        }
      }
    }
  }
}
