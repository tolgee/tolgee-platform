package io.tolgee.development.testDataBuilder.data

import io.tolgee.development.testDataBuilder.DataBuilders
import io.tolgee.development.testDataBuilder.TestDataBuilder
import io.tolgee.model.Language
import io.tolgee.model.Permission
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.key.Key
import io.tolgee.model.translation.Translation
import io.tolgee.model.translation.TranslationComment

class TranslationSingleTestData {
  private lateinit var firstComment: TranslationComment
  lateinit var secondComment: TranslationComment
  var project: Project
  private lateinit var englishLanguage: Language
  var user: UserAccount
  var pepa: UserAccount
  lateinit var aKey: Key
  lateinit var projectBuilder: DataBuilders.ProjectBuilder
  lateinit var translation: Translation

  val root: TestDataBuilder = TestDataBuilder().apply {
    user = addUserAccount {
      self {
        username = "franta"
      }
    }.self

    pepa = addUserAccount {
      self {
        username = "pepa"
      }
    }.self

    val jindra = addUserAccount {
      self {
        username = "jindra"
      }
    }

    val vojta = addUserAccount {
      self {
        username = "vojta"
      }
    }

    project = addProject {
      self {
        name = "Franta's project"
        userOwner = user
      }

      addPermission {
        self {
          project = this@addProject.self
          user = this@TranslationSingleTestData.user
          type = Permission.ProjectPermissionType.MANAGE
        }
      }

      addPermission {
        self {
          project = this@addProject.self
          user = this@TranslationSingleTestData.pepa
          type = Permission.ProjectPermissionType.EDIT
        }
      }

      addPermission {
        self {
          project = this@addProject.self
          user = jindra.self
          type = Permission.ProjectPermissionType.TRANSLATE
        }
      }

      addPermission {
        self {
          project = this@addProject.self
          user = vojta.self
          type = Permission.ProjectPermissionType.VIEW
        }
      }

      englishLanguage = addLanguage {
        self {
          name = "English"
          tag = "en"
          originalName = "English"
        }
      }.self

      addLanguage {
        self {
          name = "Czech"
          tag = "cs"
          originalName = "Čeština"
        }
      }.self

      aKey = addKey {
        self.name = "A key"

        translation = addTranslation {
          self {
            key = this@addKey.self
            language = englishLanguage
            text = "Z translation"
            state = TranslationState.REVIEWED
          }

          firstComment = addComment {
            self {
              text = "First comment"
              author = this@TranslationSingleTestData.user
            }
          }.self

          secondComment = addComment {
            self {
              text = "Second comment"
              author = this@TranslationSingleTestData.pepa
            }
          }.self
        }.self
      }.self

      projectBuilder = this
    }.self
  }
}
