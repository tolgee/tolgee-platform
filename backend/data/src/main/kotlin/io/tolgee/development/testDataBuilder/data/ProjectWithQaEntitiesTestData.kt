package io.tolgee.development.testDataBuilder.data

import io.tolgee.model.enums.qa.QaCheckType
import io.tolgee.model.enums.qa.QaIssueMessage

class ProjectWithQaEntitiesTestData : BaseTestData() {
  init {
    projectBuilder.setQaConfig()
    englishLanguageBuilder.setQaConfig()
    projectBuilder.addKey(keyName = "test-key") {
      addTranslation("en", "Hello world.").build {
        addQaIssue {
          type = QaCheckType.EMPTY_TRANSLATION
          message = QaIssueMessage.QA_EMPTY_TRANSLATION
        }
      }
    }
  }
}
