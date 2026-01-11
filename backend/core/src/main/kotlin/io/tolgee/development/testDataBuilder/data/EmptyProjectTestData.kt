package io.tolgee.development.testDataBuilder.data

import io.tolgee.development.testDataBuilder.builders.OrganizationBuilder
import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.ProjectPermissionType
import io.tolgee.model.translation.Translation

class EmptyProjectTestData {
  lateinit var franta: UserAccount
  lateinit var admin: UserAccount
  lateinit var organizationBuilder: OrganizationBuilder
  lateinit var translation: Translation
  lateinit var project: Project

  val root =
    TestDataBuilder().apply {
      addUserAccount {
        username = "admin@admin.com"
        name = "Peter Administrator"
        role = UserAccount.Role.ADMIN
        admin = this
      }.build {
        organizationBuilder = defaultOrganizationBuilder
      }

      addUserAccount {
        username = "franta"
        name = "Franta User"
        role = UserAccount.Role.USER
        franta = this
      }.build {
        organizationBuilder = defaultOrganizationBuilder
      }

      addProject {
        organizationOwner = organizationBuilder.self
        name = "project"
        project = this
      }.build {
        addPermission {
          user = franta
          type = ProjectPermissionType.EDIT
        }
      }
    }
}
