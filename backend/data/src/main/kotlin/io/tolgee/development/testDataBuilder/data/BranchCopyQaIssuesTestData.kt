package io.tolgee.development.testDataBuilder.data

import io.tolgee.model.branching.Branch
import io.tolgee.model.enums.qa.QaCheckType
import io.tolgee.model.enums.qa.QaIssueMessage
import io.tolgee.model.enums.qa.QaIssueState
import io.tolgee.model.key.Key
import io.tolgee.model.qa.TranslationQaIssue
import io.tolgee.model.translation.Translation

class BranchCopyQaIssuesTestData : BaseTestData("branch-copy-qa", "Branch copy QA test") {
  lateinit var defaultBranch: Branch
  lateinit var key: Key
  lateinit var englishTranslation: Translation
  lateinit var openIssue: TranslationQaIssue
  lateinit var virtualIgnoredIssue: TranslationQaIssue

  init {
    projectBuilder.apply {
      self.useBranching = true
      addBranch {
        name = "main"
        project = projectBuilder.self
        isProtected = true
        isDefault = true
      }.build {
        defaultBranch = self
      }
      addKey {
        name = "greeting"
        branch = defaultBranch
      }.build {
        key = self
        addTranslation {
          language = englishLanguage
          text = "Hello world."
          qaChecksStale = false
        }.build {
          englishTranslation = self
          addQaIssue {
            type = QaCheckType.PUNCTUATION_MISMATCH
            message = QaIssueMessage.QA_PUNCTUATION_ADD
            state = QaIssueState.OPEN
          }.build {
            openIssue = self
          }
          addQaIssue {
            type = QaCheckType.CHARACTER_CASE_MISMATCH
            message = QaIssueMessage.QA_CASE_CAPITALIZE
            state = QaIssueState.IGNORED
            virtual = true
          }.build {
            virtualIgnoredIssue = self
          }
        }
      }
    }
  }
}
