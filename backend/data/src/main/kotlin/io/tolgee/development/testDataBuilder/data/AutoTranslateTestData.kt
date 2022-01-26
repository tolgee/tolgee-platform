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
          self {
            usingTm = true
            usingPrimaryMtService = true
          }
        }

        addLanguage {
          self {
            name = "German"
            tag = "de"
            germanLanguage = this
          }
        }
        addLanguage {
          self {
            name = "Spanish"
            tag = "es"
            spanishLanguage = this
          }
        }
        addKey {
          self {
            name = "base-translation-doesn't-exist"
            baseTranslationNotExistKey = this
            addTranslation {
              self {
                key = this@addKey.self
                text = null
                state = TranslationState.UNTRANSLATED
                language = germanLanguage
              }
            }
            addTranslation {
              self {
                key = this@addKey.self
                text = "i am translated"
                state = TranslationState.TRANSLATED
                language = spanishLanguage
              }
            }
          }
        }
        addKey {
          self {
            name = "base-translation-untranslated"
            baseTranslationUntranslated = this
            addTranslation {
              self {
                key = this@addKey.self
                text = null
                state = TranslationState.UNTRANSLATED
                language = englishLanguage
              }
            }
            addTranslation {
              self {
                key = this@addKey.self
                text = "i am translated"
                state = TranslationState.TRANSLATED
                language = spanishLanguage
              }
            }
          }
        }
        addKey {
          self {
            name = "this-is-beautiful"
            thisIsBeautifulKey = this
            addTranslation {
              self {
                key = this@addKey.self
                text = "This is beautiful"
                state = TranslationState.TRANSLATED
                language = englishLanguage
              }
            }
            addTranslation {
              self {
                key = this@addKey.self
                text = "Es ist schön."
                state = TranslationState.TRANSLATED
                language = germanLanguage
              }
            }
          }
        }
      }
    }
  }

  fun generateManyLanguages() {
    projectBuilder.apply {
      (1..20).forEach { langNum ->
        addLanguage {
          addMtServiceConfig {
            self {
              this.targetLanguage = this@addLanguage.self
              this.primaryService = if (langNum % 2 == 0) MtServiceType.AWS else MtServiceType.GOOGLE
            }
          }
          self {
            name = "Lang $langNum"
            tag = if (langNum % 3 == 0) "cs-$langNum" else "lng-$langNum"
          }
        }
      }
    }
  }

  fun generateLanguagesWithDifferentPrimaryServices() {
    projectBuilder.apply {
      addLanguage {
        self {
          name = "Google translated Lang"
          tag = "fr"
        }
        addMtServiceConfig {
          self {
            this.targetLanguage = this@addLanguage.self
            this.primaryService = MtServiceType.GOOGLE
          }
        }
      }
      addLanguage {
        self {
          name = "AWS translated Lang"
          tag = "ar"
        }
        addMtServiceConfig {
          self {
            this.targetLanguage = this@addLanguage.self
            this.primaryService = MtServiceType.AWS
          }
        }
      }
    }
  }
}
