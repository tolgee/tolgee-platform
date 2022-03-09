package io.tolgee.development.testDataBuilder.data

import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.model.ApiKey
import io.tolgee.model.Language
import io.tolgee.model.Permission
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.ApiScope
import io.tolgee.model.translation.Translation

class LanguagePermissionsTestData {
  lateinit var project: Project
  lateinit var englishLanguage: Language
  lateinit var germanLanguage: Language
  lateinit var enOnlyUser: UserAccount
  lateinit var allLangUser: UserAccount
  lateinit var bothLangsExplicitUser: UserAccount
  lateinit var bothLangsExplicitUserApiKey: ApiKey

  lateinit var englishTranslation: Translation
  lateinit var germanTranslation: Translation

  val root: TestDataBuilder = TestDataBuilder().apply {
    addUserAccount {
      username = "en_only_user"
      enOnlyUser = this
    }

    addUserAccount {
      username = "all_lang_user"
      allLangUser = this
    }

    addUserAccount {
      username = "both_lang_user"
      bothLangsExplicitUser = this
    }

    addProject {
      project = this
      name = "Project"
    }.build {
      englishLanguage = addEnglish().self
      germanLanguage = addGerman().self

      addPermission {
        languages = mutableSetOf(englishLanguage)
        user = enOnlyUser
        type = Permission.ProjectPermissionType.TRANSLATE
      }

      addPermission {
        user = allLangUser
        type = Permission.ProjectPermissionType.TRANSLATE
      }

      addPermission {
        user = bothLangsExplicitUser
        type = Permission.ProjectPermissionType.TRANSLATE
        languages = mutableSetOf(englishLanguage, germanLanguage)
      }

      addApiKey {
        userAccount = bothLangsExplicitUser
        scopesEnum = mutableSetOf(ApiScope.TRANSLATIONS_EDIT)
        bothLangsExplicitUserApiKey = this
      }

      addKey {
        name = "key"
      }.build {
        addTranslation {
          language = englishLanguage
          text = "english_translation"
          englishTranslation = this
        }
        addTranslation {
          language = germanLanguage
          text = "german_translation"
          germanTranslation = this
        }
      }
      addKey {
        name = "key2"
      }
    }
  }
}
