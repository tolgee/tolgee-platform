package io.tolgee.development.testDataBuilder.data.dataImport

import io.tolgee.development.testDataBuilder.builders.ProjectBuilder
import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.model.Language
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.model.dataImport.Import
import io.tolgee.model.dataImport.ImportFile
import io.tolgee.model.dataImport.ImportLanguage
import io.tolgee.model.enums.ProjectPermissionType
import io.tolgee.model.key.KeyComment
import io.tolgee.model.translation.Translation

class ImportNamespacesTestData {
  lateinit var import: Import
  lateinit var english: Language
  lateinit var german: Language
  lateinit var importEnglish: ImportLanguage
  lateinit var importGerman: ImportLanguage
  lateinit var homepageImportEnglish: ImportLanguage
  lateinit var homepageImportGerman: ImportLanguage
  lateinit var project: Project
  lateinit var userAccount: UserAccount
  lateinit var projectBuilder: ProjectBuilder
  lateinit var defaultNsFile: ImportFile
  lateinit var defaultNsFile2: ImportFile
  lateinit var homepageNsFile2: ImportFile
  lateinit var existingTranslation: Translation

  val root: TestDataBuilder =
    TestDataBuilder().apply {
      createProject()
      projectBuilder
        .build {
          addImport {
            author = userAccount
            import = this
          }.build {
            addImportFile {
              name = "multilang.json"
              defaultNsFile = this
            }.build {
              importEnglish =
                addImportLanguage {
                  name = "en"
                  existingLanguage = english
                }.self
              importGerman =
                addImportLanguage {
                  name = "de"
                  existingLanguage = german
                }.self
              addImportKey {
                name = "what a key"
              }.build key@{
                addMeta {
                  comments =
                    mutableListOf(
                      KeyComment(this).apply {
                        text = "hello1"
                        author = userAccount
                      },
                    )
                }
                addImportTranslation {
                  text = "hello"
                  language = importGerman
                  key = this@key.self
                  conflict = existingTranslation
                  override = true
                  resolve()
                }
              }
            }

            addImportFile {
              name = "multilang2.json"
              defaultNsFile2 = this
            }.build {
              addImportKey {
                name = "what a key"
              }.build key@{
                addMeta {
                  comments =
                    mutableListOf(
                      KeyComment(this).apply {
                        text = "hello2"
                        author = userAccount
                      },
                    )
                }
              }
            }

            addImportFile {
              name = "another.json"
              namespace = "homepage"
              homepageNsFile2 = this
            }.build {
              homepageImportEnglish =
                addImportLanguage {
                  name = "en"
                  existingLanguage = english
                }.self
              homepageImportGerman =
                addImportLanguage {
                  name = "de"
                  existingLanguage = german
                }.self
              addImportKey {
                name = "what a key"
              }.build key@{
                addMeta {
                  comments =
                    mutableListOf(
                      KeyComment(this).apply {
                        text = "hello2"
                        author = userAccount
                      },
                    )
                }
                addImportTranslation {
                  text = "hello"
                  language = homepageImportGerman
                  key = this@key.self
                }
              }
            }
          }
        }.self
    }

  private fun TestDataBuilder.createProject() {
    userAccount =
      addUserAccount {
        username = "franta"
        name = "Frantisek Dobrota"
      }.self
    projectBuilder =
      addProject {
        name = "test"
        project = this
        useNamespaces = true
      }.build project@{
        addPermission {
          project = this@project.self
          user = this@ImportNamespacesTestData.userAccount
          type = ProjectPermissionType.MANAGE
        }
        english = addEnglish().self
        german = addGerman().self
        addKey {
          name = "what a key"
        }.setNamespace("existing-namespace")
        addKey {
          name = "what a key"
        }.setNamespace("existing-namespace2")
        addKey {
          name = "what a key"
        }.build {
          addTranslation {
            existingTranslation = this
            language = german
            this.key = this@build.self
            text = "some text!"
          }
        }
        addKey {
          name = "what a nice key"
        }
      }
  }
}
