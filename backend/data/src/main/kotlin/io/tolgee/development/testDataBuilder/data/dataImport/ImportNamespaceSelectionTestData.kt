package io.tolgee.development.testDataBuilder.data.dataImport

import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.model.Language
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.model.dataImport.ImportFile
import io.tolgee.model.enums.ProjectPermissionType

class ImportNamespaceSelectionTestData {
  lateinit var project: Project
  var userAccount: UserAccount
  lateinit var german: Language
  lateinit var flatFile: ImportFile
  lateinit var webFile: ImportFile

  val root: TestDataBuilder =
    TestDataBuilder().apply {
      userAccount =
        addUserAccount {
          username = "franta"
          name = "Frantisek Dobrota"
        }.self
      addProject {
        name = "test"
        useNamespaces = true
        project = this
      }.build project@{
        addPermission {
          project = this@project.self
          user = this@ImportNamespaceSelectionTestData.userAccount
          type = ProjectPermissionType.MANAGE
        }
        addEnglish()
        german = addGerman().self
        addKey {
          name = "existing key"
        }.setNamespace("web")
        setImportSettings {
          userAccount = this@ImportNamespaceSelectionTestData.userAccount
          createNewKeys = false
        }
        addImport {
          author = userAccount
        }.build {
          addImportFile {
            name = "flat.xlsx"
            flatFile = this
          }.build {
            val importGerman =
              addImportLanguage {
                name = "de"
                existingLanguage = german
              }.self
            addImportKey {
              name = "existing key"
              shouldBeImported = false
            }.build key@{
              addImportTranslation {
                text = "Hallo"
                language = importGerman
                key = this@key.self
              }
            }
          }
          addImportFile {
            name = "web.xlsx"
            namespace = "web"
            webFile = this
          }.build {
            val importGerman =
              addImportLanguage {
                name = "de"
                existingLanguage = german
              }.self
            addImportKey {
              name = "existing key"
              shouldBeImported = true
            }.build key@{
              addImportTranslation {
                text = "Hallo"
                language = importGerman
                key = this@key.self
              }
            }
          }
        }
      }
    }
}
