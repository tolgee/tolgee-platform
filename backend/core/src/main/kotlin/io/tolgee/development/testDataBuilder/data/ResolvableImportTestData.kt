package io.tolgee.development.testDataBuilder.data

import io.tolgee.model.Language
import io.tolgee.model.Screenshot
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.ProjectPermissionType
import io.tolgee.model.enums.Scope
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.key.Key

class ResolvableImportTestData : BaseTestData() {
  lateinit var key1and2Screenshot: Screenshot
  lateinit var key2Screenshot: Screenshot
  lateinit var enOnlyUser: UserAccount
  lateinit var viewOnlyUser: UserAccount
  lateinit var keyCreateOnlyUser: UserAccount
  lateinit var translateOnlyUser: UserAccount
  lateinit var secondLanguage: Language
  lateinit var translatorUser: UserAccount

  init {
    projectBuilder.apply {
      secondLanguage = addGerman().self

      addKey("namespace-1", "key-1") {
        addTranslation("de", "existing translation")
        key1and2Screenshot =
          addScreenshot {
            location = "My cool frame"
          }.self
      }

      addKey("namespace-1", "key-2") {
        addTranslation("en", "existing translation")
        key2Screenshot = addScreenshot {}.self
        addScreenshotReference {
          key = this@addKey.self
          screenshot = key1and2Screenshot
        }
      }
      addKey("test") {
        addTranslation("en", "existing translation")
      }

      addKey("keyWith2Translations") {
        addTranslation("en", "existing translation")
        addTranslation {
          this.language = secondLanguage
          this.text = "existing translation"
          this.outdated = false
          this.state = TranslationState.REVIEWED
        }
      }

      addAutoTranslationConfig {
        usingTm = true
        usingPrimaryMtService = true
        enableForImport = true
        targetLanguage = secondLanguage
      }
    }

    root.addUserAccount {
      username = "franta"
      enOnlyUser = this
    }

    projectBuilder.addPermission {
      user = enOnlyUser
      scopes = arrayOf(Scope.KEYS_CREATE, Scope.TRANSLATIONS_EDIT)
      translateLanguages = mutableSetOf(englishLanguage)
      type = null
    }

    root.addUserAccount {
      username = "pavel"
      viewOnlyUser = this
    }

    projectBuilder.addPermission {
      user = viewOnlyUser
      type = null
      scopes = arrayOf(Scope.TRANSLATIONS_VIEW)
    }

    root.addUserAccount {
      username = "kvetos"
      keyCreateOnlyUser = this
    }

    projectBuilder.addPermission {
      user = keyCreateOnlyUser
      type = null
      scopes = arrayOf(Scope.KEYS_CREATE)
    }

    root.addUserAccount {
      username = "jenik"
      translateOnlyUser = this
    }

    projectBuilder.addPermission {
      user = translateOnlyUser
      type = null
      scopes = arrayOf(Scope.TRANSLATIONS_EDIT)
      translateLanguages = mutableSetOf(englishLanguage)
    }

    root.addUserAccount {
      username = "translator"
      translatorUser = this
    }

    projectBuilder.addPermission {
      user = translatorUser
      type = ProjectPermissionType.TRANSLATE
    }
  }

  fun addLotOfKeys(count: Int): List<Key> {
    return (1..count).map {
      projectBuilder
        .addKey(keyName = "key-lot-$it") {
          addTranslation("en", "existing translation")
        }.self
    }
  }
}
