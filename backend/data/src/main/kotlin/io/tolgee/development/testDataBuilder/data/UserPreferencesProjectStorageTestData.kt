package io.tolgee.development.testDataBuilder.data

import io.tolgee.development.testDataBuilder.builders.ProjectBuilder
import io.tolgee.model.ApiKey
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.ProjectPermissionType
import io.tolgee.model.enums.Scope

class UserPreferencesProjectStorageTestData : BaseTestData("project_storage_user", "Project storage project") {
  lateinit var secondProjectBuilder: ProjectBuilder
  val secondProject: Project get() = secondProjectBuilder.self

  lateinit var viewer: UserAccount
  lateinit var outsider: UserAccount
  lateinit var admin: UserAccount
  lateinit var supporter: UserAccount
  lateinit var firstProjectApiKey: ApiKey
  lateinit var secondProjectApiKey: ApiKey

  init {
    root.apply {
      secondProjectBuilder =
        addProject {
          name = "Project storage second project"
          organizationOwner = userAccountBuilder.defaultOrganizationBuilder.self
        }

      viewer = addUserAccount { username = "project_storage_viewer" }.self
      projectBuilder.addPermission {
        user = viewer
        type = ProjectPermissionType.VIEW
      }

      outsider = addUserAccount { username = "project_storage_outsider" }.self

      admin =
        addUserAccount {
          username = "project_storage_admin"
          role = UserAccount.Role.ADMIN
        }.self

      supporter =
        addUserAccount {
          username = "project_storage_supporter"
          role = UserAccount.Role.SUPPORTER
        }.self

      firstProjectApiKey =
        projectBuilder
          .addApiKey {
            key = "project_storage_first_project_key"
            scopesEnum = Scope.entries.toMutableSet()
            userAccount = user
          }.self

      secondProjectApiKey =
        secondProjectBuilder
          .addApiKey {
            key = "project_storage_second_project_key"
            scopesEnum = Scope.entries.toMutableSet()
            userAccount = user
          }.self
    }
  }
}
