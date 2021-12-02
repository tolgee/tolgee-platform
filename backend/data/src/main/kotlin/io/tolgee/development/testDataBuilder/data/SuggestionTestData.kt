package io.tolgee.development.testDataBuilder.data

import io.tolgee.model.Language
import io.tolgee.model.key.Key

class SuggestionTestData : BaseTestData() {
  var germanLanguage: Language
  lateinit var beautifulKey: Key

  init {
    projectBuilder.apply {
      germanLanguage = addLanguage {
        self {
          name = "German"
          tag = "de"
          originalName = "Deutsch"
        }
      }.self

      addKey {
        self {
          name = "key 1"
        }
        addTranslation {
          self {
            language = englishLanguage
            key = this@addKey.self
            text = "Beautiful"
          }
        }
        addTranslation {
          self {
            language = germanLanguage
            key = this@addKey.self
            text = "Wunderschönen"
          }
        }
      }
      addKey {
        self {
          name = "key 2"
        }
        addTranslation {
          self {
            language = englishLanguage
            key = this@addKey.self
            text = "This is beautiful"
          }
        }
        addTranslation {
          self {
            language = germanLanguage
            key = this@addKey.self
            text = "Das ist schön"
          }
        }
      }
      addKey {
        self {
          name = "key 3"
        }
        addTranslation {
          self {
            language = englishLanguage
            key = this@addKey.self
            text = "This is different"
          }
        }
        addTranslation {
          self {
            language = germanLanguage
            key = this@addKey.self
            text = "Das ist anders"
          }
        }
      }
      addKey {
        self {
          name = "key 4"
          beautifulKey = this
        }
        addTranslation {
          self {
            language = englishLanguage
            key = this@addKey.self
            text = "Beautiful"
          }
        }
      }
    }
  }
}
