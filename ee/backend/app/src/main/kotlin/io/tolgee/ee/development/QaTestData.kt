package io.tolgee.ee.development

import io.tolgee.development.testDataBuilder.data.BaseTestData
import io.tolgee.model.Language
import io.tolgee.model.enums.qa.QaCheckSeverity
import io.tolgee.model.enums.qa.QaCheckType
import io.tolgee.model.enums.qa.QaIssueMessage
import io.tolgee.model.enums.qa.QaIssueState
import io.tolgee.model.key.Key
import io.tolgee.model.qa.ProjectQaConfig
import io.tolgee.model.translation.Translation

class QaTestData(
  includeDefaultQaConfig: Boolean = true,
) : BaseTestData() {
  lateinit var frenchLanguage: Language
  lateinit var testKey: Key
  lateinit var enTranslation: Translation
  lateinit var frTranslation: Translation

  lateinit var otherProjectKey: Key

  /** Key that only has a base (English) translation — no French translation exists. */
  lateinit var keyWithoutFrTranslation: Key

  /** Key whose FR translation has fresh (non-stale) QA checks and no QA issues. */
  lateinit var freshFrKey: Key
  lateinit var freshFrTranslation: Translation
  lateinit var freshEnTranslation: Translation

  /** Key whose FR translation has stale QA checks and no QA issues. */
  lateinit var staleFrKey: Key
  lateinit var staleFrTranslation: Translation

  /**
   * Key whose FR translation has OPEN PUNCTUATION_MISMATCH + CHARACTER_CASE_MISMATCH
   * issues seeded. Used by filter tests that need at least one key with active issues.
   */
  lateinit var keyWithIssues: Key

  /**
   * Key whose FR translation has only IGNORED QA issues. Used to verify that filters
   * which target OPEN issues correctly exclude translations whose issues have all been
   * ignored.
   */
  lateinit var ignoredOnlyKey: Key

  lateinit var germanLanguage: Language

  lateinit var keyWithDeIssues: Key

  /**
   * Spanish has QA disabled at the language level ([LanguageQaConfig.enabled] = false) while the
   * project QA feature stays on; [disabledLangTranslationWithIssues] seeds OPEN issues on it.
   */
  lateinit var spanishLanguage: Language
  lateinit var disabledLangTranslationWithIssues: Translation

  init {
    project.useQaChecks = true
    projectBuilder.build {
      frenchLanguage = addFrench().self
      val spanishBuilder =
        addLanguage {
          name = "Spanish"
          tag = "es"
          originalName = "Español"
        }
      spanishBuilder.setQaConfig { enabled = false }
      spanishLanguage = spanishBuilder.self
      // testKey: clean. No QA issues — tests that a pristine translation.
      testKey =
        addKey {
          name = "test-key"
        }.build {
          enTranslation = addTranslation("en", "Hello world.").self
          frTranslation = addTranslation("fr", "bonjour monde").self
        }.self
      keyWithIssues =
        addKey {
          name = "key-with-issues"
        }.build {
          addTranslation("en", "Hello world.")
          addTranslation("fr", "bonjour monde")
            .also { it.self.qaChecksStale = false }
            .build {
              addQaIssue {
                type = QaCheckType.PUNCTUATION_MISMATCH
                message = QaIssueMessage.QA_PUNCTUATION_ADD
                state = QaIssueState.OPEN
                positionStart = 13
                positionEnd = 13
                replacement = "."
              }
              addQaIssue {
                type = QaCheckType.CHARACTER_CASE_MISMATCH
                message = QaIssueMessage.QA_CASE_CAPITALIZE
                state = QaIssueState.OPEN
                positionStart = 0
                positionEnd = 1
                replacement = "B"
              }
            }
          disabledLangTranslationWithIssues =
            addTranslation("es", "hola mundo")
              .also { it.self.qaChecksStale = false }
              .build {
                addQaIssue {
                  type = QaCheckType.PUNCTUATION_MISMATCH
                  message = QaIssueMessage.QA_PUNCTUATION_ADD
                  state = QaIssueState.OPEN
                  positionStart = 11
                  positionEnd = 11
                  replacement = "."
                }
                addQaIssue {
                  type = QaCheckType.CHARACTER_CASE_MISMATCH
                  message = QaIssueMessage.QA_CASE_CAPITALIZE
                  state = QaIssueState.OPEN
                  positionStart = 0
                  positionEnd = 1
                  replacement = "H"
                }
              }.self
        }.self
      keyWithoutFrTranslation =
        addKey {
          name = "key-without-fr-translation"
        }.build {
          addTranslation("en", "Only English.")
        }.self
      freshFrKey =
        addKey {
          name = "fresh-fr-key"
        }.build {
          freshEnTranslation = addTranslation("en", "Fresh.").also { it.self.qaChecksStale = false }.self
          freshFrTranslation = addTranslation("fr", "Frais.").also { it.self.qaChecksStale = false }.self
        }.self
      staleFrKey =
        addKey {
          name = "stale-fr-key"
        }.build {
          addTranslation("en", "Stale.").also { it.self.qaChecksStale = false }
          staleFrTranslation = addTranslation("fr", "Périmé.").also { it.self.qaChecksStale = true }.self
        }.self
      ignoredOnlyKey =
        addKey {
          name = "ignored-only-key"
        }.build {
          addTranslation("en", "Click here.").also { it.self.qaChecksStale = false }
          addTranslation("fr", "Cliquez ici")
            .also { it.self.qaChecksStale = false }
            .build {
              addQaIssue {
                type = QaCheckType.PUNCTUATION_MISMATCH
                message = QaIssueMessage.QA_PUNCTUATION_ADD
                state = QaIssueState.IGNORED
                positionStart = 11
                positionEnd = 11
                replacement = "."
              }
            }
        }.self
      germanLanguage =
        addLanguage {
          name = "German"
          tag = "de"
          originalName = "Deutsch"
        }.self
      keyWithDeIssues =
        addKey {
          name = "key-with-de-issues"
        }.build {
          addTranslation("en", "Hello world.").also { it.self.qaChecksStale = false }
          addTranslation("de", " Hallo Welt.")
            .also { it.self.qaChecksStale = false }
            .build {
              addQaIssue {
                type = QaCheckType.SPACES_MISMATCH
                message = QaIssueMessage.QA_SPACES_LEADING_ADDED
                state = QaIssueState.OPEN
                positionStart = 0
                positionEnd = 1
                replacement = ""
              }
            }
        }.self
      if (includeDefaultQaConfig) {
        setQaConfig {
          settings =
            QaCheckType.entries
              .associateWith { type ->
                when (type) {
                  QaCheckType.SPELLING, QaCheckType.GRAMMAR -> QaCheckSeverity.OFF
                  else -> QaCheckSeverity.WARNING
                }
              }.toMutableMap()
        }
      }
    }

    root.apply {
      val otherUser =
        addUserAccount {
          username = "other_user"
        }
      addProject {
        name = "other_project"
        organizationOwner = otherUser.defaultOrganizationBuilder.self
      }.build {
        val otherEnglish =
          addLanguage {
            name = "English"
            tag = "en"
            originalName = "English"
            this@build.self.baseLanguage = this
          }
        self.baseLanguage = otherEnglish.self
        addPermission {
          project = this@build.self
          user = otherUser.self
          type = io.tolgee.model.enums.ProjectPermissionType.MANAGE
        }
        otherProjectKey =
          addKey {
            name = "secret-key"
          }.build {
            addTranslation("en", "Secret content.")
          }.self
      }
    }
  }

  fun clearAllStaleFlags() {
    projectBuilder.data.translations.forEach { it.self.qaChecksStale = false }
  }

  fun disableQaChecks() {
    project.useQaChecks = false
  }

  /**
   * Creates QA config with all check types enabled at WARNING, except SPELLING and GRAMMAR
   * which are OFF (they depend on an external LanguageTool container).
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
