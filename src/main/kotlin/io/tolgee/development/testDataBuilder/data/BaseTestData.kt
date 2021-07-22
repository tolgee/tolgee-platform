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
  var user: UserAccount

  val root: TestDataBuilder = TestDataBuilder().apply {
    user = addUserAccount {
      self {
        username = userName
      }
    }.self
    projectBuilder = addProject {
      self {
        name = projectName
        userOwner = user
      }

      addPermission {
        self {
          project = this@addProject.self
          user = this@BaseTestData.user
          type = Permission.ProjectPermissionType.MANAGE
        }
      }

      englishLanguage = addLanguage {
        self {
          name = "English"
          tag = "en"
          originalName = "English"
        }
      }.self
    }
  }
}
