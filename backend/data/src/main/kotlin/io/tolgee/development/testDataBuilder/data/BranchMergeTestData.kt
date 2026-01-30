package io.tolgee.development.testDataBuilder.data

import io.tolgee.development.testDataBuilder.builders.ProjectBuilder
import io.tolgee.model.branching.Branch
import io.tolgee.model.branching.BranchMerge
import io.tolgee.model.enums.BranchKeyMergeChangeType
import io.tolgee.model.enums.BranchKeyMergeResolutionType
import io.tolgee.model.enums.TaskState
import io.tolgee.model.enums.TaskType
import io.tolgee.model.key.Key
import io.tolgee.model.key.Tag
import io.tolgee.model.task.Task
import io.tolgee.model.translation.Label
import java.util.Date

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
  lateinit var mergedConflictBranchMerge: BranchMerge
  lateinit var tag1: Tag
  lateinit var tag2: Tag
  lateinit var tag3: Tag
  lateinit var label1: Label
  lateinit var label2: Label
  lateinit var label3: Label
  lateinit var label4: Label
  lateinit var mainTask: Task
  lateinit var featureOpenTask: Task
  lateinit var featureFinishedTask: Task
  lateinit var mergedFeatureTask: Task
  lateinit var conflictsBranchTask: Task

  companion object {
    const val UPDATE_KEY_NAME = "shared-update-key"
    const val DELETE_KEY_NAME = "shared-delete-key"
    const val CONFLICT_KEY_NAME = "shared-conflict-key"
  }

  init {
    root.apply {
      projectBuilder.apply {
        self.useBranching = true
        addBranches()
        addLabels()
        addSharedKeys()
        createMergeWithConflicts()
        addTasks()
      }
    }
  }

  private fun ProjectBuilder.addBranches() {
    mainBranch =
      addBranch {
        name = "main"
        isProtected = true
        isDefault = true
      }.build mainBranch@{
        featureBranch =
          addBranch {
            name = "feature"
            originBranch = this@mainBranch.self
            isDefault = false
            isProtected = false
          }.self
        conflictsBranch =
          addBranch {
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
      extended = true,
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

  private fun ProjectBuilder.addTasks() {
    mainTask =
      addTask {
        number = 1
        name = "Main branch task"
        type = TaskType.TRANSLATE
        state = TaskState.NEW
        project = projectBuilder.self
        language = englishLanguage
        author = user
        branch = mainBranch
      }.self

    featureOpenTask =
      addTask {
        number = 2
        name = "Feature branch open task"
        type = TaskType.TRANSLATE
        state = TaskState.NEW
        project = projectBuilder.self
        language = englishLanguage
        author = user
        branch = featureBranch
      }.self

    addTaskKey {
      task = featureOpenTask
      key = featureKeyToUpdate
      done = false
    }

    featureFinishedTask =
      addTask {
        number = 3
        name = "Feature branch finished task"
        type = TaskType.TRANSLATE
        state = TaskState.FINISHED
        project = projectBuilder.self
        language = englishLanguage
        author = user
        branch = featureBranch
      }.self

    addTaskKey {
      task = featureFinishedTask
      key = featureKeyToDelete
      done = true
    }

    mergedFeatureTask =
      addTask {
        number = 4
        name = "Merged feature task"
        type = TaskType.REVIEW
        state = TaskState.FINISHED
        project = projectBuilder.self
        language = englishLanguage
        author = user
        branch = mainBranch
        originBranchName = featureBranch.name
      }.self

    addTaskKey {
      task = mergedFeatureTask
      key = mainKeyToUpdate
      done = true
    }

    conflictsBranchTask =
      addTask {
        number = 5
        name = "Conflicts branch finished task"
        type = TaskType.REVIEW
        state = TaskState.FINISHED
        project = projectBuilder.self
        language = englishLanguage
        author = user
        branch = conflictsBranch
      }.self

    addTaskKey {
      task = conflictsBranchTask
      key = conflictsBranchKey
      done = true
    }
  }

  private fun ProjectBuilder.addMatchingKey(
    name: String,
    mainText: String,
    featureText: String = mainText,
    extended: Boolean = false,
    callback: (main: Key, feature: Key) -> Unit = { _, _ -> },
  ) {
    val mainKey =
      addKey {
        this.name = name
        this.branch = mainBranch
      }.build {
        addTranslation {
          language = englishLanguage
          text = mainText
          labels = mutableSetOf(label1, label2, label3)
        }
        if (extended) {
          tag1 = addTag("abc")
          tag2 = addTag("def")
          tag3 = addTag("ghi")
        }
      }.self

    val featureKey =
      addKey {
        this.name = name
        this.branch = featureBranch
      }.build {
        addTranslation {
          language = englishLanguage
          text = featureText
          labels = mutableSetOf(label1, label2, label3)
        }
        if (extended) {
          addTag("abc")
          addTag("def")
          addTag("ghi")
        }
      }.self

    callback(mainKey, featureKey)
  }

  private fun ProjectBuilder.createMergeWithConflicts() {
    conflictsBranchKey =
      addKey {
        name = CONFLICT_KEY_NAME
        branch = conflictsBranch
      }.build {
        addTranslation {
          language = englishLanguage
          text = "Conflict feature text"
        }
      }.self
    conflictBranchMerge =
      addBranchMerge {
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
    mergedConflictBranchMerge =
      addBranchMerge {
        sourceBranch = conflictsBranch
        targetBranch = mainBranch
        sourceRevision = conflictsBranch.revision + 6
        targetRevision = mainBranch.revision + 4
        mergedAt = Date()
      }.build {
        addChange {
          change = BranchKeyMergeChangeType.CONFLICT
          sourceKey = conflictsBranchKey
          targetKey = mainConflictKey
          resolution = BranchKeyMergeResolutionType.SOURCE
        }
      }.self
  }

  private fun ProjectBuilder.addLabels() {
    label1 =
      addLabel {
        name = "prod"
        color = "#FF0000"
        description = "Production label"
      }.self
    label2 =
      addLabel {
        name = "staging"
        color = "#00FF00"
        description = "Staging label"
      }.self
    label3 =
      addLabel {
        name = "dev"
        color = "#0000FF"
        description = "Development label"
      }.self
    label4 =
      addLabel {
        name = "test"
        color = "#FFFF00"
        description = "Test label"
      }.self
  }
}
