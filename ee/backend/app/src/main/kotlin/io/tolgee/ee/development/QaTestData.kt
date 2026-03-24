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
    project.useQaChecks = true
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
   * Creates QA config with all check types enabled at WARNING, except SPELLING and GRAMMAR
   * which are OFF (they depend on JLanguageTool which is non-deterministic in integration tests).
   */
  fun createDefaultQaConfig(): ProjectQaConfig {
    return ProjectQaConfig(
      project = project,
      settings =
        QaCheckType.entries
          .associateWith { type ->
            when (type) {
              QaCheckType.SPELLING, QaCheckType.GRAMMAR -> QaCheckSeverity.OFF
              else -> QaCheckSeverity.WARNING
            }
          }.toMutableMap(),
    )
  }
}
