package io.tolgee.development.testDataBuilder.data

import io.tolgee.model.Language
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.key.Key

class AutoTranslateTestData : BaseTestData() {
  lateinit var thisIsBeautifulKey: Key
  lateinit var germanLanguage: Language
  lateinit var spanishLanguage: Language
  lateinit var baseTranslationNotExistKey: Key
  lateinit var baseTranslationUntranslated: Key

  init {
    root.apply {
      projectBuilder.apply {
        addLanguage {
          name = "German"
          tag = "de"
          germanLanguage = this
        }
        val spanish =
          addLanguage {
            name = "Spanish"
            tag = "es"
            spanishLanguage = this
          }.self
        addAutoTranslationConfig {
          usingTm = true
          usingPrimaryMtService = true
        }
        addAutoTranslationConfig {
          usingTm = true
          usingPrimaryMtService = true
          targetLanguage = spanish
        }
        addKey {
          name = "base-translation-doesn't-exist"
          baseTranslationNotExistKey = this
        }.build buildKey@{
          addTranslation {
            key = this@buildKey.self
            text = null
            state = TranslationState.UNTRANSLATED
            language = germanLanguage
          }
          addTranslation {
            key = this@buildKey.self
            text = "i am translated"
            state = TranslationState.TRANSLATED
            language = spanishLanguage
          }
        }
        addKey {
          name = "base-translation-untranslated"
          baseTranslationUntranslated = this
        }.build buildKey@{
          addTranslation {
            key = this@buildKey.self
            text = null
            state = TranslationState.UNTRANSLATED
            language = englishLanguage
          }
          addTranslation {
            key = this@buildKey.self
            text = "i am translated"
            state = TranslationState.TRANSLATED
            language = spanishLanguage
          }
        }
        addKey {
          name = "this-is-beautiful"
          thisIsBeautifulKey = this
        }.build buildKey@{
          addTranslation {
            key = this@buildKey.self
            text = "This is beautiful"
            state = TranslationState.TRANSLATED
            language = englishLanguage
          }
          addTranslation {
            key = this@buildKey.self
            text = "Es ist schön."
            state = TranslationState.TRANSLATED
            language = germanLanguage
          }
        }
      }
    }
  }

  fun createAnotherThisIsBeautifulKey(): Key {
    return projectBuilder
      .addKey {
        name = "another-this-is-b"
      }.build {
        addTranslation {
          language = englishLanguage
          text = "This is beautiful"
        }
      }.self
  }

  fun disableAutoTranslating() {
    projectBuilder.data.autoTranslationConfigBuilders.forEach {
      it.self {
        usingPrimaryMtService = false
        usingTm = false
      }
    }
  }
}
