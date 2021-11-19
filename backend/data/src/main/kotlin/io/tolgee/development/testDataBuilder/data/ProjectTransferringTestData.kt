package io.tolgee.development.testDataBuilder.data

import io.tolgee.model.Organization
import io.tolgee.model.Permission
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.OrganizationRoleType

class ProjectTransferringTestData : BaseTestData() {
  lateinit var user2: UserAccount
  lateinit var user3: UserAccount
  lateinit var organization: Organization
  lateinit var notOwnedOrganization: Organization
  lateinit var organizationOwnedProject: Project
  lateinit var vobtahlo: UserAccount

  init {
    root.apply {
      addUserAccount {
        self {
          username = "pepik"
          name = "Josef Kajetan"
          user2 = this
        }
      }
      addUserAccount {
        self {
          username = "filip"
          name = "Filip Malecek"
          user3 = this
        }
      }
      addUserAccount {
        self {
          username = "vobtah"
          name = "Petr Vobtahlo"
          vobtahlo = this
        }
        projectBuilder.addPermission {
          self {
            user = this@addUserAccount.self
            type = Permission.ProjectPermissionType.VIEW
          }
        }
      }
      projectBuilder.addPermission {
        self {
          user = user2
          type = Permission.ProjectPermissionType.VIEW
        }
      }

      addOrganization {
        self {
          name = "Not owned organization"
          notOwnedOrganization = this
          slug = "not-owned-organization"
        }
      }

      addOrganization {
        self {
          name = "Owned organization"
          organization = this
          slug = "owned-organization"
        }
        addRole {
          self {
            type = OrganizationRoleType.OWNER
            user = this@ProjectTransferringTestData.user
          }
        }
        addProject(organizationOwner = this@addOrganization.self) {
          self {
            name = "Organization owned project"
            organizationOwnedProject = this
          }
          addPermission {
            self {
              user = this@ProjectTransferringTestData.user2
              type = Permission.ProjectPermissionType.VIEW
            }
          }
        }
      }
    }
  }
}
