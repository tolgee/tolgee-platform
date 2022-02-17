package io.tolgee.development.testDataBuilder.data

import io.tolgee.development.testDataBuilder.DataBuilders
import io.tolgee.development.testDataBuilder.TestDataBuilder
import io.tolgee.model.Language
import io.tolgee.model.Permission
import io.tolgee.model.UserAccount

open class BaseTestData(
  userName: String = "test_username",
  projectName: String = "test_project"
) {
  var projectBuilder: DataBuilders.ProjectBuilder
  lateinit var englishLanguage: Language
  lateinit var user: UserAccount

  val root: TestDataBuilder = TestDataBuilder().apply {
    addUserAccount {
      username = userName
      user = this
    }

    projectBuilder = addProject {
      name = projectName
      userOwner = user
    }.build buildProject@{
      addPermission {
        project = this@buildProject.self
        user = this@BaseTestData.user
        type = Permission.ProjectPermissionType.MANAGE
      }

      addLanguage {
        name = "English"
        tag = "en"
        originalName = "English"
        englishLanguage = this
      }

      this.self {
        baseLanguage = englishLanguage
      }
    }
  }
}
