package io.tolgee.development.testDataBuilder.data

import io.tolgee.model.Organization
import io.tolgee.model.Permission
import io.tolgee.model.Project
import io.tolgee.model.enums.OrganizationRoleType

class MtCreditsTestData : BaseTestData() {
  lateinit var organization: Organization
  lateinit var organizationProject: Project

  init {
    root.addOrganization {
      self {
        name = "Org"
        organization = this
        basePermissions = Permission.ProjectPermissionType.TRANSLATE
      }
      addRole {
        self {
          this.user = this@MtCreditsTestData.user
          this.type = OrganizationRoleType.MEMBER
        }
      }
      addMtCreditBucket {
        self {
          credits = 12000
        }
      }
    }

    root.addProject {
      self {
        name = "Organization project"
        organizationOwner = organization
        userOwner = null
        organizationProject = this
      }
    }

    root.data.userAccounts[0].addMtCreditBucket {
      self {
        credits = 15000
      }
    }
  }
}
