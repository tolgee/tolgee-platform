package io.tolgee.development.testDataBuilder.data.dataImport

import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.model.Language
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.ProjectPermissionType

class ImportCleanTestData {
  lateinit var french: Language
  lateinit var english: Language
  var project: Project
  var userAccount: UserAccount
  val projectBuilder get() = root.data.projects[0]

  val root: TestDataBuilder =
    TestDataBuilder().apply {
      userAccount =
        addUserAccount {
          username = "franta"
          name = "Frantisek Dobrota"
        }.self
      project =
        addProject { name = "test" }
          .build project@{
            addPermission {
              project = this@project.self
              user = this@ImportCleanTestData.userAccount
              type = ProjectPermissionType.MANAGE
            }

            val key =
              addKey {
                name = "key1"
              }.self
            english =
              addLanguage {
                name = "English"
                tag = "en"
              }.self
            french =
              addLanguage {
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
