package io.tolgee.development.testDataBuilder.data

import io.tolgee.model.Language

class TranslationSourceChangeStateTestData : BaseTestData() {
  lateinit var germanLanguage: Language

  init {
    projectBuilder.apply {
      germanLanguage = addGerman().self
      addKey {
        name = "A key"
      }.build {
        addTranslation {
          language = englishLanguage
          text = "English"
        }
        addTranslation {
          language = germanLanguage
          text = "German"
          outdated = true
        }
      }
      addKey {
        name = "B key"
      }.build {
        addTranslation {
          language = englishLanguage
          text = "English"
        }
        addTranslation {
          language = germanLanguage
          text = "German"
          outdated = false
        }
      }
    }
  }
}
