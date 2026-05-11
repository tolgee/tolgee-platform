package io.tolgee.development.testDataBuilder.data

import io.tolgee.model.enums.qa.QaCheckType
import io.tolgee.model.enums.qa.QaIssueMessage

/**
 * Test data covering every QA-related persistent entity on a project so the
 * hard-delete flow can be exercised against all of them at once: a
 * [ProjectQaConfig][io.tolgee.model.qa.ProjectQaConfig], a
 * [LanguageQaConfig][io.tolgee.model.qa.LanguageQaConfig] for the base
 * language, and a translation with a
 * [TranslationQaIssue][io.tolgee.model.qa.TranslationQaIssue].
 */
class ProjectWithQaEntitiesTestData : BaseTestData() {
  init {
    projectBuilder.apply {
      setQaConfig()
      addLanguageQaConfig(englishLanguage)
      addKey(keyName = "test-key") {
        addTranslation("en", "Hello world.").build {
          addQaIssue {
            type = QaCheckType.EMPTY_TRANSLATION
            message = QaIssueMessage.QA_EMPTY_TRANSLATION
          }
        }
      }
    }
  }
}
