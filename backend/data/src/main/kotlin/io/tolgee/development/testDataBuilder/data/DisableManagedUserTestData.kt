package io.tolgee.development.testDataBuilder.data

import io.tolgee.model.Organization
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.OrganizationRoleType
import io.tolgee.model.enums.ProjectPermissionType
import java.util.Date

class DisableManagedUserTestData : BaseTestData() {
  val organization: Organization get() = userAccountBuilder.defaultOrganizationBuilder.self
  val owner: UserAccount get() = user

  lateinit var managedMember: UserAccount
  lateinit var nonManagedMember: UserAccount
  lateinit var disabledNonManagedMember: UserAccount
  lateinit var projectOnlyMember: UserAccount
  lateinit var multiProjectMember: UserAccount

  lateinit var otherOrg: Organization
  lateinit var managedByOtherOrg: UserAccount

  lateinit var secondProject: Project

  init {
    root.apply {
      addUserAccountWithoutOrganization {
        username = "managed@acting.org"
        name = "Managed Member"
        managedMember = this
      }
      addUserAccountWithoutOrganization {
        username = "member@acting.org"
        name = "Non Managed Member"
        nonManagedMember = this
      }
      addUserAccountWithoutOrganization {
        username = "disabled@acting.org"
        name = "Disabled Member"
        disabledAt = Date(1700000000000)
        disabledNonManagedMember = this
      }
      addUserAccountWithoutOrganization {
        username = "projectonly@acting.org"
        name = "Project Only Member"
        projectOnlyMember = this
      }
      addUserAccountWithoutOrganization {
        username = "multiproject@acting.org"
        name = "Multi Project Member"
        multiProjectMember = this
      }

      userAccountBuilder.defaultOrganizationBuilder.build {
        addRole {
          user = managedMember
          type = OrganizationRoleType.MEMBER
          managed = true
        }
        addRole {
          user = nonManagedMember
          type = OrganizationRoleType.MEMBER
        }
        addRole {
          user = disabledNonManagedMember
          type = OrganizationRoleType.MEMBER
        }
        addRole {
          user = multiProjectMember
          type = OrganizationRoleType.MEMBER
        }
      }

      projectBuilder.build {
        addPermission {
          user = projectOnlyMember
          type = ProjectPermissionType.VIEW
        }
        addPermission {
          user = multiProjectMember
          type = ProjectPermissionType.VIEW
        }
        addPermission {
          user = disabledNonManagedMember
          type = ProjectPermissionType.VIEW
        }
        addPermission {
          user = managedMember
          type = ProjectPermissionType.VIEW
        }
      }

      // multiProjectMember must hold permissions on >=2 org projects, otherwise the GROUP BY
      // cardinality test cannot detect a join fan-out (a single project never duplicates rows).
      secondProject =
        addProject {
          name = "second_project"
          organizationOwner = userAccountBuilder.defaultOrganizationBuilder.self
        }.build {
          addPermission {
            user = multiProjectMember
            type = ProjectPermissionType.VIEW
          }
        }.self

      addUserAccountWithoutOrganization {
        username = "managed@other.org"
        name = "Managed By Other Org"
        managedByOtherOrg = this
      }
      otherOrg =
        addOrganization {
          name = "Other Org"
        }.build {
          addRole {
            user = managedByOtherOrg
            type = OrganizationRoleType.MEMBER
            managed = true
          }
        }.self
    }
  }
}
