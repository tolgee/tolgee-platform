package io.tolgee.development.testDataBuilder.data

import io.tolgee.model.Screenshot
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.Scope

class ResolvableImportTestData : BaseTestData() {
  lateinit var key1and2Screenshot: Screenshot
  lateinit var key2Screenshot: Screenshot
  lateinit var enOnlyUser: UserAccount
  lateinit var viewOnlyUser: UserAccount
  lateinit var keyCreateOnlyUser: UserAccount
  lateinit var translateOnlyUser: UserAccount

  init {
    projectBuilder.apply {
      addGerman()

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
  }
}
