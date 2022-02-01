package io.tolgee.development.testDataBuilder.data

import io.tolgee.development.testDataBuilder.builders.ProjectBuilder
import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.development.testDataBuilder.builders.UserAccountBuilder
import io.tolgee.model.Language
import io.tolgee.model.Permission
import io.tolgee.model.UserAccount

open class BaseTestData(
  userName: String = "test_username",
  projectName: String = "test_project"
) {
  var projectBuilder: ProjectBuilder
  lateinit var englishLanguage: Language
  var user: UserAccount
  var userAccountBuilder: UserAccountBuilder

  val root: TestDataBuilder = TestDataBuilder().apply {
    userAccountBuilder = addUserAccount {
      username = userName
    }
    user = userAccountBuilder.self
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
