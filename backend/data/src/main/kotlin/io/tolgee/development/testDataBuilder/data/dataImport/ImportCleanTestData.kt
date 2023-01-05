package io.tolgee.development.testDataBuilder.data.dataImport

import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.model.Permission
import io.tolgee.model.Project
import io.tolgee.model.UserAccount

class ImportCleanTestData {
  var project: Project
  var userAccount: UserAccount
  val projectBuilder get() = root.data.projects[0]

  val root: TestDataBuilder = TestDataBuilder().apply {
    userAccount = addUserAccount {
      username = "franta"
      name = "Frantisek Dobrota"
    }.self
    project = addProject { name = "test" }.build project@{
      addPermission {
        project = this@project.self
        user = this@ImportCleanTestData.userAccount
        type = Permission.ProjectPermissionType.MANAGE
      }

      val key = addKey {
        name = "key1"
      }.self
      val english = addLanguage {
        name = "English"
        tag = "en"
      }.self
      val french = addLanguage {
        name = "French"
        tag = "fr"
      }.self
      addTranslation {
        this.language = english
        this.key = key
        this.text = "test"
      }.self
      addTranslation {
        this.language = french
        this.key = key
        this.text = "test"
      }.self
    }.self
  }
}
