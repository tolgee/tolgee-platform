package io.tolgee.development.testDataBuilder.data

import io.tolgee.development.testDataBuilder.builders.ProjectBuilder
import io.tolgee.model.branching.Branch
import io.tolgee.model.key.Key
import io.tolgee.model.translation.Label
import io.tolgee.model.translation.Translation

class BranchSnapshotTestData : BaseTestData("branch_snapshot_user", "branch_snapshot_project") {
  lateinit var mainBranch: Branch
  lateinit var featureBranch: Branch
  lateinit var keyToSnapshot: Key
  lateinit var translationEn: Translation
  lateinit var label: Label

  init {
    root.apply {
      projectBuilder.apply {
        addBranches()
        addLabels()
        addKeyWithTranslationAndMeta()
      }
    }
  }

  private fun ProjectBuilder.addBranches() {
    mainBranch =
      addBranch {
        name = "main"
        isProtected = true
        isDefault = true
      }.build {
        featureBranch =
          addBranch {
            name = "feature"
            originBranch = this@build.self
            isDefault = false
            isProtected = false
          }.self
      }.self
  }

  private fun ProjectBuilder.addKeyWithTranslationAndMeta() {
    // source key on main branch
    keyToSnapshot =
      addKey {
        name = "snapshot-key"
        branch = mainBranch
      }.build {
        translationEn =
          addTranslation {
            language = englishLanguage
            text = "Snapshot text"
          }.self
        translationEn.addLabel(label)
        addTag("abc")
        addTag("def")
        addTag("ghi")
      }.self

    // matching target key on feature branch
    addKey {
      name = "snapshot-key"
      branch = featureBranch
    }.build {
      addTranslation {
        language = englishLanguage
        text = "Snapshot text"
      }
    }
  }

  private fun ProjectBuilder.addLabels() {
    label =
      addLabel {
        name = "prod"
        color = "#FF0000"
        description = "Production label"
      }.self
  }
}
