package io.tolgee.development.testDataBuilder.data

import io.tolgee.development.testDataBuilder.builders.OrganizationBuilder
import io.tolgee.model.Organization
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.OrganizationRoleType
import io.tolgee.model.enums.ProjectPermissionType

open class AppsTestData : BaseTestData("apps-test-owner@test.com", "test_project") {
  lateinit var member: UserAccount
  lateinit var otherOwner: UserAccount
  lateinit var otherOrganization: Organization
  lateinit var otherOrganizationBuilder: OrganizationBuilder
  lateinit var otherProject: Project
  lateinit var siblingProject: Project

  val organization: Organization
    get() = userAccountBuilder.defaultOrganizationBuilder.self

  init {
    root.apply {
      member = addUserAccount { username = "apps-test-member@test.com" }.self

      userAccountBuilder.defaultOrganizationBuilder.addRole {
        this.user = this@AppsTestData.member
        this.type = OrganizationRoleType.MEMBER
      }

      projectBuilder.addPermission {
        this.project = this@AppsTestData.projectBuilder.self
        this.user = this@AppsTestData.member
        this.type = ProjectPermissionType.VIEW
      }

      val siblingProjectBuilder =
        addProject {
          name = "sibling_project"
          organizationOwner = userAccountBuilder.defaultOrganizationBuilder.self
        }
      siblingProject = siblingProjectBuilder.self
      siblingProjectBuilder.addPermission {
        this.project = siblingProjectBuilder.self
        this.user = this@AppsTestData.user
        this.type = ProjectPermissionType.MANAGE
      }

      val otherOwnerBuilder = addUserAccount { username = "apps-test-other-owner@test.com" }
      otherOwner = otherOwnerBuilder.self
      otherOrganizationBuilder = otherOwnerBuilder.defaultOrganizationBuilder
      otherOrganization = otherOrganizationBuilder.self

      val otherProjectBuilder =
        addProject {
          name = "other_project"
          organizationOwner = otherOwnerBuilder.defaultOrganizationBuilder.self
        }
      otherProject = otherProjectBuilder.self
      // The main org's owner is also a member of a project in the OTHER organization, so a
      // user-context token must still stay opaque there (no cross-org membership enumeration).
      otherProjectBuilder.addPermission {
        this.project = otherProjectBuilder.self
        this.user = this@AppsTestData.user
        this.type = ProjectPermissionType.VIEW
      }
    }
  }
}
