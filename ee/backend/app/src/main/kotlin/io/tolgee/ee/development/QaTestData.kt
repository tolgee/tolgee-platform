package io.tolgee.ee.development

import io.tolgee.development.testDataBuilder.data.BaseTestData
import io.tolgee.model.Language
import io.tolgee.model.enums.qa.QaCheckSeverity
import io.tolgee.model.enums.qa.QaCheckType
import io.tolgee.model.key.Key
import io.tolgee.model.qa.ProjectQaConfig
import io.tolgee.model.translation.Translation

class QaTestData : BaseTestData() {
  lateinit var frenchLanguage: Language
  lateinit var testKey: Key
  lateinit var enTranslation: Translation
  lateinit var frTranslation: Translation

  init {
    projectBuilder.build {
      frenchLanguage = addFrench().self
      testKey =
        addKey {
          name = "test-key"
        }.build {
          enTranslation = addTranslation("en", "Hello world.").self
          frTranslation = addTranslation("fr", "bonjour monde").self
        }.self
    }
  }

  /**
   * Creates QA config with explicitly named checks.
   * New check types added in the future won't be auto-included.
   */
  fun createDefaultQaConfig(): ProjectQaConfig {
    return ProjectQaConfig(
      project = project,
      settings =
        mutableMapOf(
          QaCheckType.EMPTY_TRANSLATION to QaCheckSeverity.WARNING,
          QaCheckType.SPACES_MISMATCH to QaCheckSeverity.WARNING,
          QaCheckType.UNMATCHED_NEWLINES to QaCheckSeverity.WARNING,
          QaCheckType.CHARACTER_CASE_MISMATCH to QaCheckSeverity.WARNING,
          QaCheckType.MISSING_NUMBERS to QaCheckSeverity.WARNING,
          QaCheckType.PUNCTUATION_MISMATCH to QaCheckSeverity.WARNING,
          QaCheckType.BRACKETS_MISMATCH to QaCheckSeverity.WARNING,
          QaCheckType.BRACKETS_UNBALANCED to QaCheckSeverity.WARNING,
          QaCheckType.SPECIAL_CHARACTER_MISMATCH to QaCheckSeverity.WARNING,
          QaCheckType.DIFFERENT_URLS to QaCheckSeverity.WARNING,
          QaCheckType.INCONSISTENT_PLACEHOLDERS to QaCheckSeverity.WARNING,
          QaCheckType.INCONSISTENT_HTML to QaCheckSeverity.WARNING,
          QaCheckType.HTML_SYNTAX to QaCheckSeverity.WARNING,
          QaCheckType.ICU_SYNTAX to QaCheckSeverity.WARNING,
          QaCheckType.REPEATED_WORDS to QaCheckSeverity.WARNING,
        ),
    )
  }
}
