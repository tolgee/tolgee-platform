package io.tolgee.development.testDataBuilder.data

import io.tolgee.model.Language
import io.tolgee.model.Project
import io.tolgee.model.translationMemory.TranslationMemory
import io.tolgee.model.translationMemory.TranslationMemoryType

/**
 * Test data for the in-translations editor TM suggestion panel e2e tests.
 *
 * One project, six "edit-target" keys (each engineered for a specific scenario), two helper
 * keys that exist on the project so the project TM can produce cross-key suggestions, and
 * three shared TMs assigned to the project (default-penalty, penalized, read-disabled).
 *
 * The shared TM entry whose target text equals [BACKDATED_TARGET_TEXT] is deliberately
 * post-dated by the matching e2e controller via a native UPDATE — Spring's
 * `@LastModifiedDate` listener overwrites whatever the builder sets, so the backdating
 * has to happen after persist.
 */
class TmSuggestionsE2eTestData : BaseTestData(
  userName = "tm_suggestions_user",
  projectName = "Suggestions Project",
) {
  companion object {
    /** Source text matched by edit-target keys whose only suggestion comes from the 3-day-old
     *  shared TM entry. The controller backdates the row with this target text. */
    const val BACKDATED_TARGET_TEXT = "Čas z TM"
  }

  lateinit var czechLanguage: Language
  lateinit var sharedTmWithDate: TranslationMemory
  lateinit var penalizedSharedTm: TranslationMemory
  lateinit var noReadSharedTm: TranslationMemory

  init {
    root.apply {
      projectBuilder.apply buildProject@{
        czechLanguage =
          addLanguage {
            name = "Czech"
            tag = "cs"
            originalName = "Čeština"
          }.self

        // Helper keys — exist on the project so the project TM (virtual) carries them as
        // cross-key suggestion candidates for the "Cross-key reference" and "Multi-source"
        // edit targets below.
        addKey { name = "existing-helper-keyref" }.build {
          addTranslation {
            language = englishLanguage
            text = "Cross key reference target"
          }
          addTranslation {
            language = czechLanguage
            text = "Křížový překlad z projektu"
          }
        }
        addKey { name = "existing-helper-multi" }.build {
          addTranslation {
            language = englishLanguage
            text = "Multi source text"
          }
          addTranslation {
            language = czechLanguage
            text = "Z projektu"
          }
        }

        addEditTarget("tier-source", "Tier source text")
        addEditTarget("penalty-source", "Penalty source text")
        addEditTarget("noread-source", "Read-disabled source")
        addEditTarget("multi-source", "Multi source text")
        addEditTarget("keyref-source", "Cross key reference target")
        addEditTarget("time-source", "Time source text")
        addEditTarget("empty-source", "Truly nothing matches xyz")
      }

      val org = userAccountBuilder.defaultOrganizationBuilder

      org.addTranslationMemory {
        name = "Shared TM"
        sourceLanguageTag = "en"
        type = TranslationMemoryType.SHARED
      }.build {
        sharedTmWithDate = self
        assignProject(project) { priority = 1 }
        addEntry {
          sourceText = "Tier source text"
          targetText = "Vysoký score"
          targetLanguageTag = "cs"
        }
        addEntry {
          sourceText = "Multi source text"
          targetText = "Ze sdílené TM"
          targetLanguageTag = "cs"
        }
        addEntry {
          sourceText = "Time source text"
          targetText = BACKDATED_TARGET_TEXT
          targetLanguageTag = "cs"
        }
      }

      org.addTranslationMemory {
        name = "Penalized shared TM"
        sourceLanguageTag = "en"
        type = TranslationMemoryType.SHARED
        defaultPenalty = 30
      }.build {
        penalizedSharedTm = self
        assignProject(project) { priority = 2 }
        addEntry {
          sourceText = "Penalty source text"
          targetText = "Penalizovaný překlad"
          targetLanguageTag = "cs"
        }
      }

      org.addTranslationMemory {
        name = "No-read shared TM"
        sourceLanguageTag = "en"
        type = TranslationMemoryType.SHARED
      }.build {
        noReadSharedTm = self
        // readAccess=false means suggestions from this TM must NOT reach the panel even
        // though writes still flow in. Drives the permissions test below.
        assignProject(project) {
          priority = 3
          readAccess = false
          writeAccess = true
        }
        addEntry {
          sourceText = "Read-disabled source"
          targetText = "Should not appear"
          targetLanguageTag = "cs"
        }
      }
    }
  }

  private fun io.tolgee.development.testDataBuilder.builders.ProjectBuilder.addEditTarget(
    keyName: String,
    sourceText: String,
  ) {
    addKey { name = keyName }.build {
      addTranslation {
        language = englishLanguage
        text = sourceText
      }
    }
  }
}
