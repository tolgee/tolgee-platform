package io.tolgee.development.testDataBuilder.data

import io.tolgee.development.testDataBuilder.builders.ProjectBuilder
import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.development.testDataBuilder.builders.UserAccountBuilder
import io.tolgee.model.Language
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.ProjectPermissionType

open class BaseTestData(
  userName: String = "test_username",
  projectName: String = "test_project",
) {
  var projectBuilder: ProjectBuilder
  val project get() = projectBuilder.self

  lateinit var englishLanguage: Language
  var user: UserAccount
  var userAccountBuilder: UserAccountBuilder

  val root: TestDataBuilder =
    TestDataBuilder().apply {
      userAccountBuilder =
        addUserAccount {
          username = userName
        }

      user = userAccountBuilder.self

      projectBuilder =
        addProject {
          name = projectName
          organizationOwner = userAccountBuilder.defaultOrganizationBuilder.self
        }.build buildProject@{

          addPermission {
            project = this@buildProject.self
            user = this@BaseTestData.user
            type = ProjectPermissionType.MANAGE
          }

          addLanguage {
            name = "English"
            tag = "en"
            originalName = "English"
            englishLanguage = this
            this@buildProject.self.baseLanguage = this
          }

          this.self {
            baseLanguage = englishLanguage
          }
        }
    }
}
