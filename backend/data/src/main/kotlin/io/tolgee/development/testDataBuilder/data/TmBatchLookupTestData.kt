package io.tolgee.development.testDataBuilder.data

import io.tolgee.model.Language
import io.tolgee.model.key.Key

class TmBatchLookupTestData : BaseTestData() {
  lateinit var germanLanguage: Language
  lateinit var spanishLanguage: Language

  lateinit var sourceKey: Key
  lateinit var duplicateSourceKey: Key
  lateinit var requestedKey: Key
  lateinit var requestedKeySameText: Key
  lateinit var uniqueKey: Key
  lateinit var selfMatchKey: Key

  init {
    projectBuilder.apply {
      addLanguage {
        name = "German"
        tag = "de"
        germanLanguage = this
      }
      addLanguage {
        name = "Spanish"
        tag = "es"
        spanishLanguage = this
      }

      addKey {
        name = "source-key"
        sourceKey = this
      }.build buildKey@{
        addTranslation {
          key = this@buildKey.self
          language = englishLanguage
          text = "Hello"
        }
        addTranslation {
          key = this@buildKey.self
          language = germanLanguage
          text = "Hallo"
        }
        addTranslation {
          key = this@buildKey.self
          language = spanishLanguage
          text = "Hola"
        }
      }

      addKey {
        name = "duplicate-source-key"
        duplicateSourceKey = this
      }.build buildKey@{
        addTranslation {
          key = this@buildKey.self
          language = englishLanguage
          text = "Hello"
        }
        addTranslation {
          key = this@buildKey.self
          language = germanLanguage
          text = "Hallo (alternative)"
        }
      }

      addKey {
        name = "requested-key"
        requestedKey = this
      }.build buildKey@{
        addTranslation {
          key = this@buildKey.self
          language = englishLanguage
          text = "Hello"
        }
      }

      addKey {
        name = "requested-key-same-text"
        requestedKeySameText = this
      }.build buildKey@{
        addTranslation {
          key = this@buildKey.self
          language = englishLanguage
          text = "Hello"
        }
      }

      addKey {
        name = "unique-key"
        uniqueKey = this
      }.build buildKey@{
        addTranslation {
          key = this@buildKey.self
          language = englishLanguage
          text = "Nothing else matches this"
        }
      }

      addKey {
        name = "self-match-key"
        selfMatchKey = this
      }.build buildKey@{
        addTranslation {
          key = this@buildKey.self
          language = englishLanguage
          text = "Unique-for-self"
        }
        addTranslation {
          key = this@buildKey.self
          language = germanLanguage
          text = "Pre-existing German"
        }
      }
    }
  }
}
