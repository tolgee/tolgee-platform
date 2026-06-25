package io.tolgee.development.testDataBuilder.data

import io.tolgee.model.branching.Branch

/**
 * A branched source project for the import branch-fidelity tests: a default branch, a feature branch
 * created from it (`originBranch`), a key on each, and a task bound to the feature branch with its
 * `originBranchName` populated. Exercises default-branch reconciliation, `Key.branch` remap,
 * `Branch.originBranch` self-reference, and `Task.branch` remap.
 */
class ProjectImportBranchedSourceTestData :
  BaseTestData(userName = "branched-source-owner", projectName = "branched-source-project") {
  lateinit var defaultBranch: Branch
  lateinit var featureBranch: Branch

  val defaultKeyName = "default-branch-key"
  val featureKeyName = "feature-branch-key"
  val branchTaskName = "feature-branch-task"

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
    }
  }
}
