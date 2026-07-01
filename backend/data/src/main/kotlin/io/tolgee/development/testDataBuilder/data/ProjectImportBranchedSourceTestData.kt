package io.tolgee.development.testDataBuilder.data

import io.tolgee.development.testDataBuilder.builders.ProjectBuilder
import io.tolgee.model.Screenshot
import io.tolgee.model.branching.Branch
import io.tolgee.model.key.Key

/**
 * A branched source project for the import branch-fidelity tests: a default branch, a feature branch
 * created from it (`originBranch`), a key on each, and a task bound to the feature branch with its
 * `originBranchName` populated. Exercises default-branch reconciliation, `Key.branch` remap,
 * `Branch.originBranch` self-reference, and `Task.branch` remap.
 *
 * It also carries keys **present on both branches** (same name/namespace, as real branch copies) so a
 * caller can run `BranchSnapshotService.createInitialSnapshot(project.id, defaultBranch, featureBranch)`
 * and get real branch merge-baseline snapshots: [sharedKeyName] (with a label, keyMeta custom + tag,
 * and a screenshot shared with its default copy, so its snapshot's jsonb columns are non-empty) and
 * [danglingKeyName1] / [danglingKeyName2] (whose feature copies a test can soft-delete to make the
 * snapshot's `branchKeyId` dangle).
 */
class ProjectImportBranchedSourceTestData :
  BaseTestData(userName = "branched-source-owner", projectName = "branched-source-project") {
  lateinit var defaultBranch: Branch
  lateinit var featureBranch: Branch

  lateinit var sharedDefaultKey: Key
  lateinit var sharedFeatureKey: Key
  lateinit var sharedScreenshot: Screenshot
  lateinit var danglingFeatureKey1: Key
  lateinit var danglingFeatureKey2: Key

  val defaultKeyName = "default-branch-key"
  val featureKeyName = "feature-branch-key"
  val branchTaskName = "feature-branch-task"

  val sharedKeyName = "shared-key"
  val sharedTranslationText = "shared value"
  val sharedLabelName = "shared-label"
  val sharedTagName = "shared-tag"
  val sharedMetaDescription = "shared-desc"
  val sharedCustom: Map<String, Any?> = mapOf("shared-c" to "shared-v")

  val danglingKeyName1 = "dangling-key-1"
  val danglingKeyName2 = "dangling-key-2"

  init {
    projectBuilder.apply {
      self.useBranching = true
      defaultBranch =
        addBranch {
          name = "main"
          isDefault = true
          project = projectBuilder.self
        }.self
      featureBranch =
        addBranch {
          name = "feature"
          isDefault = false
          originBranch = defaultBranch
          project = projectBuilder.self
        }.self

      addKey(keyName = defaultKeyName).build {
        addTranslation("en", "on default")
        self.branch = defaultBranch
      }
      addKey(keyName = featureKeyName).build {
        addTranslation("en", "on feature")
        self.branch = featureBranch
      }
      addTask {
        name = branchTaskName
        number = 1
        language = englishLanguage
        project = projectBuilder.self
        branch = featureBranch
        originBranchName = "feature"
      }

      addSharedKey()
      danglingFeatureKey1 = addDanglingKey(danglingKeyName1)
      danglingFeatureKey2 = addDanglingKey(danglingKeyName2)
    }
  }

  private fun ProjectBuilder.addSharedKey() {
    val label =
      addLabel {
        name = sharedLabelName
        color = "#123456"
        project = projectBuilder.self
      }.self

    // One builder for both copies so the default copy provably matches the feature copy — the equality
    // the snapshot baseline relies on (a merge that mutates only the feature copy must classify UPDATE).
    fun addCopy(branch: Branch) =
      addKey(keyName = sharedKeyName).build {
        self.branch = branch
        addTranslation("en", sharedTranslationText).self.labels = mutableSetOf(label)
        addMeta {
          description = sharedMetaDescription
          custom = sharedCustom.toMutableMap()
        }
        addTag(sharedTagName)
      }

    val featureBuilder = addCopy(featureBranch)
    sharedFeatureKey = featureBuilder.self
    sharedScreenshot = featureBuilder.addScreenshot { }.self

    sharedDefaultKey = addCopy(defaultBranch).self
    addScreenshotReference {
      key = sharedDefaultKey
      screenshot = sharedScreenshot
    }
  }

  private fun ProjectBuilder.addDanglingKey(keyName: String): Key {
    addKeyOnBranch(keyName, defaultBranch)
    return addKeyOnBranch(keyName, featureBranch).self
  }

  private fun ProjectBuilder.addKeyOnBranch(
    keyName: String,
    branch: Branch,
  ) = addKey(keyName = keyName).build {
    self.branch = branch
    addTranslation("en", "$keyName value")
  }
}
