package io.tolgee.development.testDataBuilder.data

import io.tolgee.development.testDataBuilder.builders.ProjectBuilder
import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.model.Language
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.ProjectPermissionType
import io.tolgee.model.enums.Scope
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.key.Key
import io.tolgee.model.translation.Translation
import io.tolgee.model.translation.TranslationComment

class TranslationCommentsTestData {
  lateinit var firstComment: TranslationComment
  lateinit var secondComment: TranslationComment
  var project: Project
  lateinit var englishLanguage: Language
  lateinit var czechLanguage: Language
  lateinit var user: UserAccount
  var pepa: UserAccount
  lateinit var aKey: Key
  lateinit var bKey: Key
  lateinit var projectBuilder: ProjectBuilder
  lateinit var translation: Translation

  val root: TestDataBuilder =
    TestDataBuilder().apply {
      var userAccountBuilder =
        addUserAccount {
          username = "franta"
          user = this
        }
      pepa =
        addUserAccount {
          username = "pepa"
        }.self

      project =
        addProject {
          name = "Franta's project"
          organizationOwner = userAccountBuilder.defaultOrganizationBuilder.self
        }.build {
          addPermission {
            user = this@TranslationCommentsTestData.user
            type = ProjectPermissionType.MANAGE
          }

          addPermission {
            user = this@TranslationCommentsTestData.pepa
            type = null
            scopes = arrayOf(Scope.TRANSLATIONS_VIEW)
          }

          englishLanguage =
            addLanguage {
              name = "English"
              tag = "en"
              originalName = "English"
            }.self

          czechLanguage =
            addLanguage {
              name = "Czech"
              tag = "cs"
              originalName = "Čeština"
            }.self

          self.baseLanguage = czechLanguage

          addKey {
            name = "A key"
            this@TranslationCommentsTestData.aKey = this
          }.build {
            addTranslation {
              language = englishLanguage
              text = "Z translation"
              state = TranslationState.REVIEWED
              this@TranslationCommentsTestData.translation = this
            }.build {
              firstComment =
                addComment {
                  text = "First comment"
                }.self
              secondComment =
                addComment {
                  text = "Second comment"
                }.self
            }

            addTranslation {
              language = czechLanguage
              text = "Z překlad"
              state = TranslationState.REVIEWED
              this@TranslationCommentsTestData.translation = this
            }.build {
              firstComment =
                addComment {
                  text = "First comment"
                }.self
              secondComment =
                addComment {
                  text = "Second comment"
                }.self
            }
          }

          addKey {
            name = "B key"
            this@TranslationCommentsTestData.bKey = this
          }
          projectBuilder = this
        }.self
    }

  fun addE2eTestData() {
    this.root.apply {
      val jindra =
        addUserAccount {
          username = "jindra"
        }
      val vojta =
        addUserAccount {
          username = "vojta"
        }
      projectBuilder.apply {
        addPermission {
          project = projectBuilder.self
          user = jindra.self
          type = ProjectPermissionType.TRANSLATE
          translateLanguages = mutableSetOf(englishLanguage)
        }
        addPermission {
          project = projectBuilder.self
          user = vojta.self
          type = ProjectPermissionType.VIEW
        }
        addKey {
          name = "C key"
        }.build {
          addTranslation {
            language = englishLanguage
            text = "Bla translation"
            state = TranslationState.REVIEWED
          }.build {
            firstComment =
              addComment {
                text = "First comment"
                author = jindra.self
              }.self
            secondComment =
              addComment {
                text = "Second comment"
              }.self
          }.self
        }.self

        addKey {
          name = "D key"
        }.build {
          addTranslation {
            language = englishLanguage
            text = "Bla translation"
            state = TranslationState.REVIEWED
          }.build {
            (1..50).forEach {
              addComment {
                text = "comment $it"
                author = jindra.self
              }
            }
          }.self
        }.self
      }
    }
  }
}
