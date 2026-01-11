package io.tolgee.development.testDataBuilder.data

import io.tolgee.development.testDataBuilder.builders.ProjectBuilder
import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.model.Language
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.ProjectPermissionType
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.key.Key
import io.tolgee.model.translation.Translation
import io.tolgee.model.translation.TranslationComment

class TranslationSingleTestData {
  private lateinit var firstComment: TranslationComment
  lateinit var secondComment: TranslationComment
  var project: Project
  private lateinit var englishLanguage: Language
  lateinit var user: UserAccount
  var pepa: UserAccount
  lateinit var aKey: Key
  lateinit var projectBuilder: ProjectBuilder
  lateinit var translation: Translation

  val root: TestDataBuilder =
    TestDataBuilder().apply {
      val userAccountBuilder =
        addUserAccount {
          username = "franta"
          user = this
        }

      pepa =
        addUserAccount {
          username = "pepa"
        }.self

      val jindra =
        addUserAccount {

          username = "jindra"
        }

      val vojta =
        addUserAccount {
          username = "vojta"
        }

      project =
        addProject {
          name = "Franta's project"
          organizationOwner = userAccountBuilder.defaultOrganizationBuilder.self
        }.build {
          addPermission {
            user = this@TranslationSingleTestData.user
            type = ProjectPermissionType.MANAGE
          }

          addPermission {
            user = this@TranslationSingleTestData.pepa
            type = ProjectPermissionType.EDIT
          }

          addPermission {
            user = jindra.self
            type = ProjectPermissionType.TRANSLATE
          }

          addPermission {
            user = vojta.self
            type = ProjectPermissionType.VIEW
          }

          englishLanguage =
            addLanguage {
              name = "English"
              tag = "en"
              originalName = "English"
            }.self

          addLanguage {
            name = "Czech"
            tag = "cs"
            originalName = "Čeština"
          }

          val keyBuilder =
            addKey {
              name = "A key"
              aKey = this
            }

          keyBuilder.apply {
            translation =
              addTranslation {
                key = aKey
                language = englishLanguage
                text = "Z translation"
                state = TranslationState.REVIEWED
              }.build {
                firstComment =
                  addComment {
                    text = "First comment"
                    author = this@TranslationSingleTestData.user
                  }.self
                secondComment =
                  addComment {
                    text = "Second comment"
                    author = this@TranslationSingleTestData.pepa
                  }.self
              }.self
          }

          projectBuilder = this
        }.self
    }
}
