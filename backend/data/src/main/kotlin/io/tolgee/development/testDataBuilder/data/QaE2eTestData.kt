package io.tolgee.development.testDataBuilder.data

import io.tolgee.development.testDataBuilder.builders.ProjectBuilder
import io.tolgee.model.Language
import io.tolgee.model.enums.ProjectPermissionType
import io.tolgee.model.enums.qa.QaCheckSeverity
import io.tolgee.model.enums.qa.QaCheckType
import io.tolgee.model.enums.qa.QaIssueMessage
import io.tolgee.model.enums.qa.QaIssueState
import io.tolgee.model.qa.ProjectQaConfig

class QaE2eTestData : BaseTestData() {
  lateinit var frenchLanguage: Language
  lateinit var qaConfig: ProjectQaConfig
  lateinit var disabledProjectBuilder: ProjectBuilder

  init {
    project.useQaChecks = true

    projectBuilder.build {
      frenchLanguage =
        addLanguage {
          name = "French"
          tag = "fr"
          originalName = "Français"
        }.self

      // key_placeholder_issue: missing {count} in FR translation
      addKey {
        name = "key_placeholder_issue"
      }.build {
        addTranslation("en", "Hello {name}, you have {count} items")
        addTranslation("fr", "Bonjour {name}, vous avez items").build {
          addQaIssue {
            type = QaCheckType.INCONSISTENT_PLACEHOLDERS
            message = QaIssueMessage.QA_PLACEHOLDERS_MISSING
            state = QaIssueState.OPEN
            params = """{"placeholder":"{count}"}"""
          }
        }
      }

      // key_punctuation_issue: missing ! in FR
      addKey {
        name = "key_punctuation_issue"
      }.build {
        addTranslation("en", "Hello world!")
        addTranslation("fr", "Bonjour le monde").build {
          addQaIssue {
            type = QaCheckType.PUNCTUATION_MISMATCH
            message = QaIssueMessage.QA_PUNCTUATION_ADD
            state = QaIssueState.OPEN
            positionStart = 17
            positionEnd = 17
            replacement = "!"
          }
        }
      }

      // key_spacing_issue: leading spaces in FR
      addKey {
        name = "key_spacing_issue"
      }.build {
        addTranslation("en", "Hello world")
        addTranslation("fr", "  Bonjour le monde").build {
          addQaIssue {
            type = QaCheckType.SPACES_MISMATCH
            message = QaIssueMessage.QA_SPACES_LEADING_ADDED
            state = QaIssueState.OPEN
            positionStart = 0
            positionEnd = 2
            replacement = ""
          }
        }
      }

      // key_case_issue: lowercase start in FR
      addKey {
        name = "key_case_issue"
      }.build {
        addTranslation("en", "Hello World")
        addTranslation("fr", "bonjour monde").build {
          addQaIssue {
            type = QaCheckType.CHARACTER_CASE_MISMATCH
            message = QaIssueMessage.QA_CASE_CAPITALIZE
            state = QaIssueState.OPEN
            positionStart = 0
            positionEnd = 1
            replacement = "B"
          }
        }
      }

      // key_no_issues: clean translation
      addKey {
        name = "key_no_issues"
      }.build {
        addTranslation("en", "Simple text")
        addTranslation("fr", "Texte simple")
      }

      // key_ignored_issue: IGNORED punctuation issue
      addKey {
        name = "key_ignored_issue"
      }.build {
        addTranslation("en", "Click here.")
        addTranslation("fr", "Cliquez ici").build {
          addQaIssue {
            type = QaCheckType.PUNCTUATION_MISMATCH
            message = QaIssueMessage.QA_PUNCTUATION_ADD
            state = QaIssueState.IGNORED
            positionStart = 11
            positionEnd = 11
            replacement = "."
          }
        }
      }

      // key_multiple_issues: 4 OPEN issues on FR
      addKey {
        name = "key_multiple_issues"
      }.build {
        addTranslation("en", "Hello {name}!")
        addTranslation("fr", "  bonjour").build {
          addQaIssue {
            type = QaCheckType.SPACES_MISMATCH
            message = QaIssueMessage.QA_SPACES_LEADING_ADDED
            state = QaIssueState.OPEN
            positionStart = 0
            positionEnd = 2
            replacement = ""
          }
          addQaIssue {
            type = QaCheckType.INCONSISTENT_PLACEHOLDERS
            message = QaIssueMessage.QA_PLACEHOLDERS_MISSING
            state = QaIssueState.OPEN
            params = """{"placeholder":"{name}"}"""
          }
          addQaIssue {
            type = QaCheckType.PUNCTUATION_MISMATCH
            message = QaIssueMessage.QA_PUNCTUATION_ADD
            state = QaIssueState.OPEN
            positionStart = 9
            positionEnd = 9
            replacement = "!"
          }
          addQaIssue {
            type = QaCheckType.CHARACTER_CASE_MISMATCH
            message = QaIssueMessage.QA_CASE_CAPITALIZE
            state = QaIssueState.OPEN
            positionStart = 2
            positionEnd = 3
            replacement = "B"
          }
        }
      }

      // key_correctable: correctable punctuation issue
      addKey {
        name = "key_correctable"
      }.build {
        addTranslation("en", "Welcome!")
        addTranslation("fr", "Bienvenue").build {
          addQaIssue {
            type = QaCheckType.PUNCTUATION_MISMATCH
            message = QaIssueMessage.QA_PUNCTUATION_ADD
            state = QaIssueState.OPEN
            positionStart = 9
            positionEnd = 9
            replacement = "!"
          }
        }
      }
    }

    // Mark all translations as not stale — the seeded QA issues represent the final
    // "after batch job" state.
    projectBuilder.data.translations.forEach { it.self.qaChecksStale = false }

    // Create QA config: all checks enabled at WARNING except SPELLING/GRAMMAR
    qaConfig =
      ProjectQaConfig(
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

    // Disabled QA project
    root.apply {
      disabledProjectBuilder =
        addProject {
          name = "Disabled QA Project"
          organizationOwner = userAccountBuilder.defaultOrganizationBuilder.self
          useQaChecks = false
        }.build {
          addPermission {
            project = this@build.self
            user = this@QaE2eTestData.user
            type = ProjectPermissionType.MANAGE
          }

          addLanguage {
            name = "English"
            tag = "en"
            originalName = "English"
            this@build.self.baseLanguage = this
          }

          addLanguage {
            name = "French"
            tag = "fr"
            originalName = "Français"
          }

          addKey {
            name = "disabled_key"
          }.build {
            addTranslation("en", "Test")
            addTranslation("fr", "Test")
          }
        }
    }
  }
}
