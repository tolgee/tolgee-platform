package io.tolgee.development.testDataBuilder.data

import io.tolgee.model.Project
import io.tolgee.model.branching.Branch
import io.tolgee.model.enums.BranchKeyMergeChangeType
import io.tolgee.model.enums.TranslationSuggestionState
import io.tolgee.model.enums.qa.QaCheckType
import io.tolgee.model.enums.qa.QaIssueMessage
import io.tolgee.model.enums.qa.QaIssueState
import io.tolgee.model.key.Key

/**
 * A wipe target for the project import tests: the main project pre-populated with content that must be
 * gone after a mirror import (a key + translation, a label, a task, a default + feature branch, and the
 * branch merge/snapshot rows hanging off them), plus a separate sibling project in the same organization
 * whose content must stay completely untouched (blast-radius isolation). The merge/snapshot rows exist so
 * the clear-in-place FK ordering is exercised — they FK key/branch with no DB cascade, so the wipe must
 * delete them before keys/branches or it FK-violates.
 */
class ProjectImportTargetTestData :
  BaseTestData(userName = "import-target-owner", projectName = "import-target-project") {
  val targetProject: Project get() = project
  lateinit var targetBranch: Branch
  lateinit var targetOldKey: Key
  lateinit var siblingProject: Project

  val oldKeyName = "old-target-key"
  val oldLabelName = "old-target-label"
  val oldTaskName = "old-target-task"
  val oldTargetSuggestionText = "old-target-suggestion"
  val oldTargetQaReplacement = "old-target-qa"
  val siblingKeyName = "sibling-key"
  val siblingLabelName = "sibling-label"

  init {
    projectBuilder.apply {
      self.useBranching = true
      targetOldKey =
        addKey(keyName = oldKeyName)
          .build {
            addTranslation("en", "old target value").build {
              addQaIssue {
                type = QaCheckType.EMPTY_TRANSLATION
                message = QaIssueMessage.QA_EMPTY_TRANSLATION
                state = QaIssueState.IGNORED
                replacement = oldTargetQaReplacement
              }
            }
            addSuggestion {
              language = englishLanguage
              author = user
              translation = oldTargetSuggestionText
              state = TranslationSuggestionState.ACTIVE
            }
          }.self
      addLabel {
        name = oldLabelName
        color = "#abcdef"
        project = projectBuilder.self
      }
      // Locals (not the class's `targetBranch` field) so the BranchMerge lambda's `sourceBranch`/
      // `targetBranch` receiver members aren't shadowed by an outer property of the same name.
      val main =
        addBranch {
          name = "main"
          isDefault = true
          project = projectBuilder.self
        }.self
      val feature =
        addBranch {
          name = "target-feature"
          isDefault = false
          originBranch = main
          project = projectBuilder.self
        }.self
      targetBranch = feature
      addTask {
        name = oldTaskName
        number = 1
        language = englishLanguage
        project = projectBuilder.self
      }

      addBranchMerge {
        sourceBranch = feature
        targetBranch = main
      }.addChange {
        sourceKey = targetOldKey
        change = BranchKeyMergeChangeType.UPDATE
      }
      // Child translation/keyMeta snapshots so the clear-in-place path (and assertCleared) wipes all three
      // snapshot tables against non-empty rows, not just branch_key_snapshot.
      addKeySnapshot {
        branch = feature
        name = "snapshot"
        originalKeyId = 0
        branchKeyId = 0
      }.build {
        addTranslationSnapshot {
          language = "en"
          value = "snapshot value"
        }
        addKeyMetaSnapshot {
          description = "snapshot desc"
        }
      }
    }

    siblingProject =
      root
        .addProject {
          name = "import-sibling-project"
          organizationOwner = userAccountBuilder.defaultOrganizationBuilder.self
        }.build sibling@{
          addLanguage {
            name = "English"
            tag = "en"
            originalName = "English"
            this@sibling.self.baseLanguage = this
          }
          addKey(keyName = siblingKeyName).build { addTranslation("en", "sibling value") }
          addLabel {
            name = siblingLabelName
            color = "#fedcba"
            project = this@sibling.self
          }
        }.self
  }
}
