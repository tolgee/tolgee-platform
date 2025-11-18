package io.tolgee.development.testDataBuilder.data

import io.tolgee.development.testDataBuilder.builders.ProjectBuilder
import io.tolgee.model.branching.Branch
import io.tolgee.model.branching.BranchMerge
import io.tolgee.model.enums.BranchKeyMergeChangeType
import io.tolgee.model.enums.BranchKeyMergeResolutionType
import io.tolgee.model.key.Key

class BranchMergeTestData : BaseTestData("branch_merge", "Project prepared for branch merge tests") {
  lateinit var mainBranch: Branch
  lateinit var featureBranch: Branch
  lateinit var conflictsBranch: Branch
  lateinit var mainKeyToUpdate: Key
  lateinit var featureKeyToUpdate: Key
  lateinit var mainKeyToDelete: Key
  lateinit var featureKeyToDelete: Key
  lateinit var mainConflictKey: Key
  lateinit var featureConflictKey: Key
  lateinit var conflictsBranchKey: Key
  lateinit var conflictBranchMerge: BranchMerge

  companion object {
    const val UPDATE_KEY_NAME = "shared-update-key"
    const val DELETE_KEY_NAME = "shared-delete-key"
    const val CONFLICT_KEY_NAME = "shared-conflict-key"
  }

  init {
    root.apply {
      projectBuilder.apply {
        addBranches()
        addSharedKeys()
        createMergeWithConflicts()
      }
    }
  }

  private fun ProjectBuilder.addBranches() {
    mainBranch = addBranch {
      name = "main"
      isProtected = true
      isDefault = true
    }.build mainBranch@{
      featureBranch = addBranch {
        name = "feature"
        originBranch = this@mainBranch.self
        isDefault = false
        isProtected = false
      }.self
      conflictsBranch = addBranch {
        name = "conflicts"
        originBranch = this@mainBranch.self
        isDefault = false
        isProtected = false
      }.self
    }.self
  }

  private fun ProjectBuilder.addSharedKeys() {
    addMatchingKey(
      UPDATE_KEY_NAME,
      mainText = "Original base text",
    ) { mainKey, featureKey ->
      mainKeyToUpdate = mainKey
      featureKeyToUpdate = featureKey
    }

    addMatchingKey(
      DELETE_KEY_NAME,
      mainText = "Text to delete",
    ) { mainKey, featureKey ->
      mainKeyToDelete = mainKey
      featureKeyToDelete = featureKey
    }

    addMatchingKey(
      name = "stable-key",
      mainText = "Stable text",
    )

    addMatchingKey(
      name = CONFLICT_KEY_NAME,
      mainText = "Conflict base text",
    ) { mainKey, featureKey ->
      mainConflictKey = mainKey
      featureConflictKey = featureKey
    }
  }

  private fun ProjectBuilder.addMatchingKey(
    name: String,
    mainText: String,
    featureText: String = mainText,
    callback: (main: Key, feature: Key) -> Unit = { _, _ -> }
  ) {
    val mainKey = addKey {
      this.name = name
      this.branch = mainBranch
    }.build {
      addTranslation {
        language = englishLanguage
        text = mainText
      }
    }.self

    val featureKey = addKey {
      this.name = name
      this.branch = featureBranch
    }.build {
      addTranslation {
        language = englishLanguage
        text = featureText
      }
    }.self

    callback(mainKey, featureKey)
  }

  private fun ProjectBuilder.createMergeWithConflicts() {
    conflictsBranchKey = addKey {
      name = CONFLICT_KEY_NAME
      branch = conflictsBranch
    }.build {
      addTranslation {
        language = englishLanguage
        text = "Conflict feature text"
      }
    }.self
    conflictBranchMerge = addBranchMerge {
      sourceBranch = conflictsBranch
      targetBranch = mainBranch
      sourceRevision = conflictsBranch.revision + 6
      targetRevision = mainBranch.revision + 4
    }.build {
      addChange {
        change = BranchKeyMergeChangeType.CONFLICT
        sourceKey = conflictsBranchKey
        targetKey = mainConflictKey
        resolution = BranchKeyMergeResolutionType.SOURCE
      }
    }.self
  }
}
