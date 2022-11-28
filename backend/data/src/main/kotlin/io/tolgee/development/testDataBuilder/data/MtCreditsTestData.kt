package io.tolgee.development.testDataBuilder.data

import io.tolgee.model.Organization
import io.tolgee.model.Permission.ProjectPermissionType.TRANSLATE
import io.tolgee.model.Project
import io.tolgee.model.enums.OrganizationRoleType

class MtCreditsTestData : BaseTestData() {
  lateinit var organization: Organization
  lateinit var organizationProject: Project

  init {
    root.addOrganization {
      name = "Org"
      organization = this
      basePermission!!.type = TRANSLATE
      basePermission!!.scopes = TRANSLATE.availableScopes
    }.apply {
      addRole {
        this.user = this@MtCreditsTestData.user
        this.type = OrganizationRoleType.MEMBER
      }
      addMtCreditBucket {
        credits = 12000
      }
    }

    root.addProject {
      name = "Organization project"
      organizationOwner = organization
      organizationProject = this
    }
  }
}
