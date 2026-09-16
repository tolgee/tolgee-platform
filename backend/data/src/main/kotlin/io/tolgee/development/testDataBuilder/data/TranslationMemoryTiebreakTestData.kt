package io.tolgee.development.testDataBuilder.data

import io.tolgee.model.Language
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.key.Key
import io.tolgee.model.translation.Translation

/**
 * Every "wrong" candidate is declared first and its target text sorts alphabetically first, so
 * a query with no tiebreak picks it — reorder or rename and the tests pass without the fix.
 */
class TranslationMemoryTiebreakTestData : BaseTestData() {
  lateinit var germanLanguage: Language
  lateinit var helloTargetKey: Key
  lateinit var morningTargetKey: Key
  lateinit var morningStaleTranslation: Translation

  init {
    projectBuilder.apply {
      germanLanguage =
        addLanguage {
          name = "German"
          tag = "de"
        }.self

      addKey("hello-unreviewed") {
        addTranslation("en", "Welcome")
        addTranslation("de", "Welcome").self.state = TranslationState.TRANSLATED
      }
      addKey("hello-reviewed") {
        addTranslation("en", "Welcome")
        addTranslation("de", "Willkommen").self.state = TranslationState.REVIEWED
      }
      helloTargetKey =
        addKey("hello-target") {
          addTranslation("en", "Welcome")
          addTranslation("de", "")
        }.self

      addKey("morning-stale") {
        addTranslation("en", "Good morning")
        morningStaleTranslation = addTranslation("de", "Einen guten Morgen").self
        morningStaleTranslation.state = TranslationState.TRANSLATED
      }
      addKey("morning-fresh") {
        addTranslation("en", "Good morning")
        addTranslation("de", "Guten Morgen").self.state = TranslationState.TRANSLATED
      }
      morningTargetKey =
        addKey("morning-target") {
          addTranslation("en", "Good morning")
          addTranslation("de", "")
        }.self
    }
  }
}
