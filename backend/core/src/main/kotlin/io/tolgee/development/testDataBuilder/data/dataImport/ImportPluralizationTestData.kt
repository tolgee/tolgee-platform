package io.tolgee.development.testDataBuilder.data.dataImport

import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.model.UserAccount
import io.tolgee.model.dataImport.Import
import io.tolgee.model.enums.ProjectPermissionType

class ImportPluralizationTestData {
  var userAccount: UserAccount

  val root: TestDataBuilder =
    TestDataBuilder().apply {
      userAccount =
        addUserAccount {
          username = "franta"
          name = "Frantisek Dobrota"
        }.self
    }

  lateinit var import: Import

  val projectBuilder =
    root.addProject { name = "test" }.build project@{
      addPermission {
        project = this@project.self
        user = this@ImportPluralizationTestData.userAccount
        type = ProjectPermissionType.MANAGE
      }

      val english = addEnglish()
      val czech = addCzech()

      addKey {
        name = "existing plural key"
        isPlural = true
        pluralArgName = "count"
      }.build {
        addTranslation("en", "{count, plural, one {one} other {other}}")
      }

      addKey {
        name = "existing non plural key"
        isPlural = false
      }.build {
        addTranslation("en", "I am not a plural!")
      }

      addKey {
        name = "existing non plural key 2"
        isPlural = false
      }.build {
        addTranslation("en", "I am not a plural!")
      }

      val importBuilder =
        addImport {
          author = userAccount
        }.build {
          addImportFile {
            name = "multilang.json"
          }.build {
            val importEnglish =
              addImportLanguage {
                name = "en"
                existingLanguage = english.self
              }.self

            val importCzech =
              addImportLanguage {
                name = "cs"
                existingLanguage = czech.self
              }

            addImportTranslation {
              this.language = importCzech.self
              this.key =
                addImportKey {
                  name = "existing plural key"
                }.self
              this.conflict = null
              this.text = "No plural"
            }.self

            addImportTranslation {
              this.language = importCzech.self
              this.key =
                addImportKey {
                  name = "existing non plural key"
                  pluralArgName = "count"
                }.self
              this.conflict = null
              isPlural = true
              this.text = "{count, plural, one {one} other {other}}"
            }.self

            addImportTranslation {
              this.language = importCzech.self
              this.key =
                addImportKey {
                  name = "existing non plural key 2"
                }.self
              this.conflict = null
              isPlural = false
              this.text = "Nejsem plurál!"
            }.self
          }
        }
      import = importBuilder.self
    }
}
