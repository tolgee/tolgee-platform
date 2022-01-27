package io.tolgee.development.testDataBuilder.data

import io.tolgee.constants.MtServiceType
import io.tolgee.model.Language
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.key.Key

class AutoTranslateTestData : BaseTestData() {
  lateinit var thisIsBeautifulKey: Key
  lateinit var germanLanguage: Language
  lateinit var spanishLanguage: Language
  lateinit var baseTranslationNotExistKey: Key
  lateinit var baseTranslationUntranslated: Key
  val project get() = projectBuilder.self

  init {
    root.apply {
      projectBuilder.apply {
        addAutoTranslationConfig {
          usingTm = true
          usingPrimaryMtService = true
        }
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

  fun generateManyLanguages() {
    projectBuilder.apply {
      (1..20).forEach { langNum ->
        addLanguage {
          name = "Lang $langNum"
          tag = if (langNum % 3 == 0) "cs-$langNum" else "lng-$langNum"

          addMtServiceConfig {
            this.targetLanguage = this@addLanguage
            this.primaryService = if (langNum % 2 == 0) MtServiceType.AWS else MtServiceType.GOOGLE
          }
        }
      }
    }
  }

  fun generateLanguagesWithDifferentPrimaryServices() {
    projectBuilder.apply {
      addLanguage {
        name = "Google translated Lang"
        tag = "fr"
        addMtServiceConfig {
          this.targetLanguage = this@addLanguage
          this.primaryService = MtServiceType.GOOGLE
        }
      }
      addLanguage {
        name = "AWS translated Lang"
        tag = "ar"
        addMtServiceConfig {
          this.targetLanguage = this@addLanguage
          this.primaryService = MtServiceType.AWS
        }
      }
    }
  }
}
