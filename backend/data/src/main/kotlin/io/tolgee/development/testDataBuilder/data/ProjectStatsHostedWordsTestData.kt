package io.tolgee.development.testDataBuilder.data

import io.tolgee.model.Language
import io.tolgee.model.enums.TranslationState

class ProjectStatsHostedWordsTestData :
  BaseTestData(
    "project_stats_hosted_words_user",
    "Project stats hosted words",
  ) {
  var germanLanguage: Language

  init {
    projectBuilder.apply {
      germanLanguage =
        addLanguage {
          name = "German"
          tag = "de"
          originalName = "Deutsch"
        }.self

      addKey { name = "a" }.build {
        addTranslation {
          language = englishLanguage
          text = "one two three"
          state = TranslationState.REVIEWED
        }
        addTranslation {
          language = germanLanguage
          text = "eins"
          state = TranslationState.TRANSLATED
        }
      }

      addKey { name = "b" }.build {
        addTranslation {
          language = englishLanguage
          text = "four five"
          state = TranslationState.TRANSLATED
        }
        addTranslation {
          language = germanLanguage
          text = ""
        }
      }
    }
  }
}
