package io.tolgee.development.testDataBuilder.data.dataImport

import io.tolgee.development.testDataBuilder.builders.ProjectBuilder
import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.model.Language
import io.tolgee.model.Permission
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.model.dataImport.Import
import io.tolgee.model.dataImport.ImportLanguage
import io.tolgee.model.key.KeyComment

class ImportNamespacesTestData {
  lateinit var import: Import
  lateinit var english: Language
  lateinit var german: Language
  lateinit var importEnglish: ImportLanguage
  lateinit var importGerman: ImportLanguage

  lateinit var project: Project
  lateinit var userAccount: UserAccount
  lateinit var projectBuilder: ProjectBuilder

  val root: TestDataBuilder = TestDataBuilder().apply {
    createProject()
    projectBuilder.build {
      addImport {
        author = userAccount
        import = this
      }.build {

        addImportFile {
          name = "multilang.json"
        }.build {
          importEnglish = addImportLanguage {
            name = "en"
            existingLanguage = english
          }.self
          importGerman = addImportLanguage {
            name = "de"
            existingLanguage = german
          }.self
          addImportKey {
            name = "what a key"
          }.build key@{
            addMeta {
              comments = mutableListOf(
                KeyComment(this).apply {
                  text = "hello1"
                  author = userAccount
                }
              )
            }
            addImportTranslation {
              text = "hello"
              language = importGerman
              key = this@key.self
            }
          }
        }

        addImportFile {
          name = "multilang2.json"
        }.build {
          addImportKey {
            name = "what a key"
          }.build key@{
            addMeta {
              comments = mutableListOf(
                KeyComment(this).apply {
                  text = "hello2"
                  author = userAccount
                }
              )
            }
          }
        }

        addImportFile {
          name = "another.json"
          namespace = "homepage"
        }.build {
          importEnglish = addImportLanguage {
            name = "en"
            existingLanguage = english
          }.self
          importGerman = addImportLanguage {
            name = "de"
            existingLanguage = german
          }.self
          addImportKey {
            name = "what a key"
          }.build key@{
            addMeta {
              comments = mutableListOf(
                KeyComment(this).apply {
                  text = "hello2"
                  author = userAccount
                }
              )
            }
            addImportTranslation {
              text = "hello"
              language = importGerman
              key = this@key.self
            }
          }
        }
      }
    }.self
  }

  private fun TestDataBuilder.createProject() {
    userAccount = addUserAccount {
      username = "franta"
      name = "Frantisek Dobrota"
    }.self
    projectBuilder = addProject {
      name = "test"
      project = this
    }.build project@{
      addPermission {
        project = this@project.self
        user = this@ImportNamespacesTestData.userAccount
        type = Permission.ProjectPermissionType.MANAGE
      }
      val key = addKey {
        name = "what a key"
      }.self
      addKey {
        name = "what a nice key"
      }.self
      english = addLanguage {
        name = "English"
        tag = "en"
      }.self
      german = addLanguage {
        name = "German"
        tag = "de"
      }.self
    }
  }
}
