package io.tolgee.ee.service.branching

import io.tolgee.AbstractSpringTest
import io.tolgee.development.testDataBuilder.data.BranchMergeTestData
import io.tolgee.dtos.request.LanguageRequest
import io.tolgee.dtos.request.branching.ResolveBranchMergeConflictRequest
import io.tolgee.dtos.request.key.CreateKeyDto
import io.tolgee.dtos.request.validators.exceptions.ValidationException
import io.tolgee.ee.repository.EeSubscriptionRepository
import io.tolgee.ee.repository.TaskRepository
import io.tolgee.ee.repository.branching.BranchMergeRepository
import io.tolgee.ee.repository.branching.BranchRepository
import io.tolgee.ee.service.LabelServiceImpl
import io.tolgee.ee.service.eeSubscription.EeSubscriptionServiceImpl
import io.tolgee.exceptions.NotFoundException
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.model.Language
import io.tolgee.model.Screenshot
import io.tolgee.model.branching.Branch
import io.tolgee.model.branching.BranchMerge
import io.tolgee.model.enums.BranchKeyMergeChangeType
import io.tolgee.model.enums.BranchKeyMergeResolutionType
import io.tolgee.model.enums.TaskState
import io.tolgee.model.key.Key
import io.tolgee.model.key.Tag
import io.tolgee.model.key.screenshotReference.KeyScreenshotReference
import io.tolgee.model.task.Task
import io.tolgee.model.translation.Label
import io.tolgee.model.translation.Translation
import io.tolgee.repository.KeyRepository
import io.tolgee.repository.KeyScreenshotReferenceRepository
import io.tolgee.repository.ScreenshotRepository
import io.tolgee.testing.assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.repository.findByIdOrNull

@SpringBootTest
class BranchMergeServiceTest : AbstractSpringTest() {
  @Autowired
  lateinit var branchService: BranchServiceImpl

  @Autowired
  lateinit var branchSnapshotService: BranchSnapshotService

  @Autowired
  lateinit var branchRepository: BranchRepository

  @Autowired
  lateinit var branchMergeRepository: BranchMergeRepository

  @Autowired
  lateinit var keyRepository: KeyRepository

  @Autowired
  lateinit var labelService: LabelServiceImpl

  @Autowired
  lateinit var taskRepository: TaskRepository

  @Autowired
  lateinit var screenshotRepository: ScreenshotRepository

  @Autowired
  lateinit var keyScreenshotReferenceRepository: KeyScreenshotReferenceRepository

  @Autowired
  lateinit var eeSubscriptionServiceImpl: EeSubscriptionServiceImpl

  @Autowired
  lateinit var eeSubscriptionRepository: EeSubscriptionRepository

  private lateinit var testData: BranchMergeTestData

  private val additionKeyName = "feature-only-key"
  private val additionValue = "Feature branch only value"
  private val updatedValue = "Feature branch updated text"

  @BeforeEach
  fun setup() {
    // Clear any existing subscription to avoid key limit issues from other tests
    executeInNewTransaction {
      eeSubscriptionRepository.deleteAll()
    }
    testData = BranchMergeTestData()
    testDataService.saveTestData(testData.root)
    authenticateAs(testData.user)
    branchSnapshotService.createInitialSnapshot(
      testData.project.id,
      testData.mainBranch,
      testData.featureBranch,
    )
  }

  @AfterEach
  fun cleanup() {
    clearAuthentication()
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `dry-run update from origin offers the origin change as an update on the child`() {
    updateKeyTranslation(testData.mainKeyToUpdate, "Hotfix on main")
    val merge = dryRunUpdateFromMain()
    merge.isUpdate.assert.isTrue()
    val update = merge.changes.single { it.change == BranchKeyMergeChangeType.UPDATE }
    update.sourceKey!!
      .id.assert
      .isEqualTo(testData.mainKeyToUpdate.id)
    update.targetKey!!
      .id.assert
      .isEqualTo(testData.featureKeyToUpdate.id)
    merge.changes
      .filter { it.change != BranchKeyMergeChangeType.UPDATE }
      .assert
      .isEmpty()
  }

  @Test
  fun `dry-run update from origin leaves a child-only key alone`() {
    createFeatureOnlyKey()
    val merge = dryRunUpdateFromMain()
    merge.changes.assert.isEmpty()
  }

  @Test
  fun `dry-run update from origin does not resurrect a key deleted on the child`() {
    deleteFeatureKey()
    val merge = dryRunUpdateFromMain()
    merge.changes.assert.isEmpty()
  }

  @Test
  fun `dry-run detects additions, updates and deletions`() {
    val merge = prepareMergeScenario()

    val additions = merge.changes.filter { it.change == BranchKeyMergeChangeType.ADD }
    val deletions = merge.changes.filter { it.change == BranchKeyMergeChangeType.DELETE }
    val updates = merge.changes.filter { it.change == BranchKeyMergeChangeType.UPDATE }

    additions.assert.hasSize(1)
    additions
      .first()
      .sourceKey!!
      .name.assert
      .isEqualTo(additionKeyName)
    additions
      .first()
      .resolution.assert
      .isEqualTo(BranchKeyMergeResolutionType.SOURCE)

    deletions.assert.hasSize(1)
    deletions
      .first()
      .targetKey!!
      .name.assert
      .isEqualTo(BranchMergeTestData.DELETE_KEY_NAME)

    updates.assert.hasSize(1)
    updates
      .first()
      .sourceKey!!
      .name.assert
      .isEqualTo(BranchMergeTestData.UPDATE_KEY_NAME)
    updates
      .first()
      .resolution.assert
      .isEqualTo(BranchKeyMergeResolutionType.SOURCE)
  }

  @Test
  fun `dry-run marks key as conflict when translations are changed on both branches`() {
    val mainConflictValue = "Main side conflict change"
    val featureConflictValue = "Feature side conflict change"

    updateKeyTranslation(testData.mainConflictKey, mainConflictValue)
    updateKeyTranslation(testData.featureConflictKey, featureConflictValue)

    val merge =
      branchService.dryRunMerge(
        testData.featureBranch.refresh()!!,
        testData.mainBranch.refresh()!!,
      )

    val conflicts = merge.changes.filter { it.change == BranchKeyMergeChangeType.CONFLICT }
    conflicts.assert.hasSize(1)
    conflicts
      .first()
      .sourceKey!!
      .name.assert
      .isEqualTo(BranchMergeTestData.CONFLICT_KEY_NAME)
    conflicts
      .first()
      .targetKey!!
      .name.assert
      .isEqualTo(BranchMergeTestData.CONFLICT_KEY_NAME)
    conflicts
      .first()
      .resolution.assert
      .isNull()
  }

  @Test
  fun `dry-run detects language replacement even when translation count stays equal`() {
    prepareFeatureKeyLanguageReplacement("de", "German", "Neue Ubersetzung")

    val merge =
      branchService.dryRunMerge(
        testData.featureBranch.refresh()!!,
        testData.mainBranch.refresh()!!,
      )

    merge.changes
      .filter { it.change == BranchKeyMergeChangeType.UPDATE }
      .mapNotNull { it.sourceKey?.name }
      .assert
      .contains(BranchMergeTestData.UPDATE_KEY_NAME)
  }

  @Test
  fun `dry-run treats deleted and re-added key as update`() {
    reAddFeatureDeletedKey()

    val merge =
      branchService.dryRunMerge(
        testData.featureBranch.refresh()!!,
        testData.mainBranch.refresh()!!,
      )

    merge.changes
      .filter { it.change == BranchKeyMergeChangeType.ADD }
      .mapNotNull { it.sourceKey?.name }
      .assert
      .doesNotContain(BranchMergeTestData.DELETE_KEY_NAME)

    merge.changes
      .filter { it.change == BranchKeyMergeChangeType.UPDATE }
      .mapNotNull { it.sourceKey?.name }
      .assert
      .contains(BranchMergeTestData.DELETE_KEY_NAME)
  }

  @Test
  fun `apply merge - propagates additions, updates and deletions`() {
    val merge = prepareMergeScenario()
    applyMerge(merge)

    testData.mainBranch.findKey(additionKeyName).let {
      it.assert.isNotNull()
      it!!.enTranslation().assert.isEqualTo(additionValue)
    }

    testData.mainKeyToUpdate
      .enTranslation()
      .assert
      .isEqualTo(updatedValue)

    testData.mainBranch
      .findKey(testData.mainKeyToDelete.name)
      .assert
      .isNull()

    executeInNewTransaction {
      val finalized = branchMergeRepository.findByIdOrNull(merge.id)!!
      finalized.mergedAt.assert.isNotNull()
      finalized.changes.assert.hasSize(0)
    }
  }

  @Test
  fun `apply merge - keeps branch and resets snapshot`() {
    val merge = prepareMergeScenario()
    branchService.applyMerge(testData.project.id, merge.id, false)

    val refreshedBranch = testData.featureBranch.refresh()!!
    refreshedBranch.deletedAt.assert.isNull()
    refreshedBranch.deletedAt.assert.isNull()
    testData.featureOpenTask
      .refresh()
      .state.assert
      .isEqualTo(TaskState.NEW)
    testData.featureFinishedTask
      .refresh()
      .state.assert
      .isEqualTo(TaskState.FINISHED)

    // dry-run again and validate no changes are present
    val newMerge = dryRunFeatureBranchMerge()
    newMerge.changes.size.assert
      .isEqualTo(0)
  }

  @Test
  fun `apply merge - soft-deletes the target key so it can be restored`() {
    val merge = prepareMergeScenario()
    applyMerge(merge)
    val deleted = keyRepository.findById(testData.mainKeyToDelete.id).orElseThrow()
    deleted.deletedAt.assert.isNotNull()
    testData.mainBranch
      .findKey(testData.mainKeyToDelete.name)
      .assert
      .isNull()
  }

  @Test
  fun `apply merge - a restored key is not deleted again by the next merge`() {
    branchService.applyMerge(testData.project.id, prepareMergeScenario().id, false)
    keyService.restoreKeys(listOf(testData.mainKeyToDelete.id), testData.project.id)
    val next = dryRunFeatureBranchMerge()
    next.changes
      .filter { it.change == BranchKeyMergeChangeType.DELETE }
      .assert
      .isEmpty()
  }

  @Test
  fun `apply merge - never deletes a protected source branch`() {
    val merge = prepareMergeScenario()
    branchService.setProtected(testData.project.id, testData.featureBranch.id, true)
    branchService.refreshMerge(testData.project.id, merge.id)
    branchService.applyMerge(testData.project.id, merge.id, true)
    testData.featureBranch
      .refresh()!!
      .deletedAt.assert
      .isNull()
  }

  @Test
  fun `apply merge - update from origin never deletes a branch`() {
    updateKeyTranslation(testData.mainKeyToUpdate, "Hotfix on main")
    val merge = dryRunUpdateFromMain()
    branchService.applyMerge(testData.project.id, merge.id, true)
    testData.mainBranch
      .refresh()!!
      .deletedAt.assert
      .isNull()
    testData.featureBranch
      .refresh()!!
      .deletedAt.assert
      .isNull()
    testData.featureKeyToUpdate
      .refresh()
      .enTranslation()
      .assert
      .isEqualTo("Hotfix on main")
  }

  @Test
  fun `apply merge - update from origin advances the child snapshot`() {
    updateKeyTranslation(testData.mainKeyToUpdate, "Hotfix on main")
    val merge = dryRunUpdateFromMain()
    branchService.applyMerge(testData.project.id, merge.id, false)
    dryRunUpdateFromMain().changes.assert.isEmpty()
  }

  @Test
  fun `update from origin then promote reports no change`() {
    updateKeyTranslation(testData.mainKeyToUpdate, "Hotfix on main")
    val update = dryRunUpdateFromMain()
    update.changes.assert.hasSize(1)
    branchService.applyMerge(testData.project.id, update.id, false)
    dryRunFeatureBranchMerge().changes.assert.isEmpty()
  }

  @Test
  fun `conflict resolved as keep target is not offered again`() {
    updateKeyTranslation(testData.mainConflictKey, "Main side")
    updateKeyTranslation(testData.featureConflictKey, "Feature side")
    val merge = dryRunFeatureBranchMerge()
    val conflict = merge.changes.single { it.change == BranchKeyMergeChangeType.CONFLICT }
    branchService.resolveConflict(
      testData.project.id,
      merge.id,
      ResolveBranchMergeConflictRequest(conflict.id, BranchKeyMergeResolutionType.TARGET),
    )
    branchService.applyMerge(testData.project.id, merge.id, false)
    testData.mainConflictKey
      .refresh()
      .enTranslation()
      .assert
      .isEqualTo("Main side")
    dryRunFeatureBranchMerge().changes.assert.isEmpty()
  }

  @Test
  fun `key deleted on child survives an update and is offered as deletion by the next promote`() {
    deleteFeatureKey()
    val update = dryRunUpdateFromMain()
    branchService.applyMerge(testData.project.id, update.id, false)
    branchSnapshotService
      .getSnapshotKeys(testData.featureBranch.id)
      .map { it.name }
      .assert
      .contains(BranchMergeTestData.DELETE_KEY_NAME)
    val promote = dryRunFeatureBranchMerge()
    promote.changes
      .single { it.change == BranchKeyMergeChangeType.DELETE }
      .targetKey!!
      .name.assert
      .isEqualTo(BranchMergeTestData.DELETE_KEY_NAME)
  }

  @Test
  fun `update from origin deletes a key deleted on origin and the next promote reports no change`() {
    keyService.hardDelete(testData.mainKeyToDelete.id)
    val update = dryRunUpdateFromMain()
    update.changes
      .single()
      .change.assert
      .isEqualTo(BranchKeyMergeChangeType.DELETE)
    branchService.applyMerge(testData.project.id, update.id, false)
    keyRepository
      .findById(testData.featureKeyToDelete.id)
      .orElseThrow()
      .deletedAt.assert
      .isNotNull()
    dryRunFeatureBranchMerge().changes.assert.isEmpty()
  }

  @Test
  fun `apply merge - update of a nested branch never deletes its non-default origin`() {
    val sub = branchService.createBranch(testData.project.id, "sub", testData.featureBranch.id, testData.user)
    updateKeyTranslation(testData.featureKeyToUpdate, "Feature hotfix")
    val merge = branchService.dryRunMerge(testData.featureBranch.refresh()!!, sub.refresh()!!)
    branchService.applyMerge(testData.project.id, merge.id, true)
    testData.featureBranch
      .refresh()!!
      .deletedAt.assert
      .isNull()
    sub
      .refresh()!!
      .deletedAt.assert
      .isNull()
    sub
      .findKey(testData.featureKeyToUpdate.name)!!
      .enTranslation()
      .assert
      .isEqualTo("Feature hotfix")
  }

  @Test
  fun `apply merge - moves finished and cancels unfinished tasks when deleting branch`() {
    val merge = prepareMergeScenario()
    branchService.applyMerge(testData.project.id, merge.id, true)

    waitForNotThrowing(timeout = 10000, pollTime = 100) {
      branchRepository.findByIdOrNull(testData.featureBranch.id).assert.isNull()
    }

    testData.featureOpenTask
      .refresh()
      .let { task ->
        task.state.assert.isEqualTo(TaskState.CANCELED)
        task.branch!!
          .id.assert
          .isEqualTo(testData.mainBranch.id)
        task.originBranchName.assert
          .isEqualTo(testData.featureBranch.name)
      }
    testData.featureFinishedTask
      .refresh()
      .let { task ->
        task.state.assert.isEqualTo(TaskState.FINISHED)
        task.branch!!
          .id.assert
          .isEqualTo(testData.mainBranch.id)
        task.originBranchName.assert
          .isEqualTo(testData.featureBranch.name)
      }
  }

  @Test
  fun `merge view reports uncompleted task count`() {
    val merge = prepareMergeScenario()

    branchService
      .getBranchMergeView(testData.project.id, merge.id)
      .uncompletedTasksCount
      .assert
      .isEqualTo(1)
  }

  @Test
  fun `apply merge - tags merge correctly`() {
    val mainRevisionBefore = testData.mainBranch.refresh()!!.revision
    val featureRevisionBefore = testData.featureBranch.refresh()!!.revision

    // remove one tag from each key
    removeTagFromKey(testData.mainKeyToUpdate, testData.tag1)
    removeTagFromKey(testData.featureKeyToUpdate, testData.tag2)
    // add one to the feature key
    addTagToKey(testData.featureKeyToUpdate, "xyz")

    // Wait for async branch revision updates to complete
    waitForNotThrowing(timeout = 10000, pollTime = 100) {
      testData.mainBranch
        .refresh()!!
        .revision.assert
        .isGreaterThan(mainRevisionBefore)
      testData.featureBranch
        .refresh()!!
        .revision.assert
        .isGreaterThan(featureRevisionBefore)
    }

    dryRunAndMergeFeatureBranch()

    // the main key should have one tag
    val key =
      testData.mainKeyToUpdate
        .refresh()
    // only tag3 and xyz remains after merge
    key
      .keyMeta!!
      .tags
      .let { tags ->
        tags.assert.hasSize(2)
        tags.map { it.name }.assert.containsExactlyInAnyOrder("ghi", "xyz")
      }
  }

  @Test
  fun `apply merge - labels merge correctly`() {
    val mainTranslation = getTranslation(testData.mainKeyToUpdate, testData.englishLanguage)
    val featureTranslation = getTranslation(testData.featureKeyToUpdate, testData.englishLanguage)

    val mainRevisionBefore = testData.mainBranch.refresh()!!.revision
    val featureRevisionBefore = testData.featureBranch.refresh()!!.revision

    setLabels(mainTranslation, testData.label2, testData.label3)
    setLabels(featureTranslation, testData.label1, testData.label3, testData.label4)

    // Wait for async branch revision updates to complete
    waitForNotThrowing(timeout = 10000, pollTime = 100) {
      testData.mainBranch
        .refresh()!!
        .revision.assert
        .isGreaterThan(mainRevisionBefore)
      testData.featureBranch
        .refresh()!!
        .revision.assert
        .isGreaterThan(featureRevisionBefore)
    }

    dryRunAndMergeFeatureBranch()

    val mergedTranslation =
      getTranslation(testData.mainKeyToUpdate.refresh(), testData.englishLanguage)
    mergedTranslation.labels
      .let { labels ->
        labels.assert.hasSize(2)
        labels.assert.containsExactlyInAnyOrder(testData.label3, testData.label4)
        labels
          .map { it.name }
          .assert
          .containsExactlyInAnyOrder("dev", "test")
      }
  }

  @Test
  fun `apply merge - screenshots merge correctly`() {
    val mainScreenshot = addScreenshotReference(testData.mainKeyToUpdate)
    val featureScreenshot = addScreenshotReference(testData.featureKeyToUpdate)

    dryRunAndMergeFeatureBranch()

    val mergedReferences = getScreenshotReferences(testData.mainKeyToUpdate.id)
    mergedReferences.assert.hasSize(2)
    mergedReferences
      .map { it.screenshot }
      .assert
      .containsExactlyInAnyOrder(mainScreenshot, featureScreenshot)
  }

  @Test
  fun `dry-run does not re-add key when target deleted but source unchanged`() {
    deleteTargetKey()

    val merge = dryRunFeatureBranchMerge()

    merge.changes
      .filter { it.change == BranchKeyMergeChangeType.ADD }
      .mapNotNull { it.sourceKey?.name }
      .assert
      .doesNotContain(BranchMergeTestData.DELETE_KEY_NAME)
  }

  @Test
  fun `dry-run reports no changes when target key is deleted and source unchanged`() {
    deleteTargetKey()

    val merge = dryRunFeatureBranchMerge()

    merge.changes.assert.isEmpty()
  }

  @Test
  fun `dry-run handles target key deleted and re-created with same name`() {
    val originalName = reCreateTargetKeyWithSameName()

    val merge = dryRunFeatureBranchMerge()

    val changes =
      merge.changes.filter {
        it.sourceKey?.name == originalName || it.targetKey?.name == originalName
      }
    changes.assert.isNotEmpty()
  }

  @Test
  fun `dry-run detects key rename in source branch`() {
    renameFeatureKey("renamed-key")

    val merge = dryRunFeatureBranchMerge()

    val updates = merge.changes.filter { it.change == BranchKeyMergeChangeType.UPDATE }
    updates.assert.isNotEmpty()
  }

  @Test
  fun `dry-run detects key moved to different namespace`() {
    moveFeatureKeyToNamespace("new-namespace")

    val merge = dryRunFeatureBranchMerge()

    val changes =
      merge.changes.filter {
        it.sourceKey?.id == testData.featureKeyToUpdate.id
      }
    changes.assert.isNotEmpty()
  }

  @Test
  fun `dry-run handles namespace deleted and re-created with same name`() {
    prepareNamespaceRecreation()

    val merge = dryRunFeatureBranchMerge()

    merge.changes.assert.isNotEmpty()
  }

  @Test
  fun `dry-run detects conflict when both branches add translations for new language`() {
    addDifferentTranslationsForNewLanguage("fr", "French", "Main French text", "Feature French text")

    val merge = dryRunFeatureBranchMerge()

    val changes =
      merge.changes.filter {
        it.sourceKey?.name == BranchMergeTestData.UPDATE_KEY_NAME
      }
    changes.assert.isNotEmpty()
  }

  @Test
  fun `dry-run detects conflict when both branches create same key name`() {
    val sharedName = "same-name-new-key"
    createKey(sharedName, "Main value", testData.mainBranch)
    createKey(sharedName, "Feature value", testData.featureBranch)

    val merge = dryRunFeatureBranchMerge()

    val conflicts = merge.changes.filter { it.change == BranchKeyMergeChangeType.CONFLICT }
    conflicts.assert.hasSize(1)
    conflicts
      .first()
      .sourceKey!!
      .name.assert
      .isEqualTo(sharedName)
    conflicts
      .first()
      .targetKey!!
      .name.assert
      .isEqualTo(sharedName)
    conflicts
      .first()
      .resolution.assert
      .isNull()
  }

  @Test
  fun `dry-run detects conflict when both branches change screenshot references`() {
    addScreenshotsToBothBranches()

    val merge = dryRunFeatureBranchMerge()

    val changes =
      merge.changes.filter {
        it.sourceKey?.name == BranchMergeTestData.UPDATE_KEY_NAME
      }

    changes.assert.isNotEmpty()
  }

  @Test
  fun `dry-run shows no changes when both branches have identical key with namespace`() {
    val merge = dryRunFeatureBranchMerge()

    // Should have no changes for the namespaced key since both branches have identical key
    val namespacedKeyChanges =
      merge.changes.filter {
        it.sourceKey?.name == BranchMergeTestData.NAMESPACED_KEY_NAME ||
          it.targetKey?.name == BranchMergeTestData.NAMESPACED_KEY_NAME
      }
    namespacedKeyChanges.assert.isEmpty()
  }

  @Test
  fun `apply merge - adds translation when source has language that target does not have`() {
    val featureRevisionBefore = testData.featureBranch.refresh()!!.revision

    val germanLanguage = addProjectLanguage("de", "German")
    val germanValue = "Deutsche Übersetzung"

    // Add German translation only to feature branch key
    val featureKey = keyService.get(testData.featureKeyToUpdate.id)
    val featureTranslation = translationService.getOrCreate(featureKey, germanLanguage)
    translationService.setTranslationText(featureTranslation, germanValue)

    // Main branch key should not have the German translation
    translationService
      .find(testData.mainKeyToUpdate, germanLanguage)
      .orElse(null)
      .assert
      .isNull()

    // Wait for async branch revision updates to complete
    waitForNotThrowing(timeout = 10000, pollTime = 100) {
      testData.featureBranch
        .refresh()!!
        .revision.assert
        .isGreaterThan(featureRevisionBefore)
    }

    dryRunAndMergeFeatureBranch()

    // After merge, main branch key should have the German translation
    val mergedTranslation =
      translationService
        .find(testData.mainKeyToUpdate.refresh(), germanLanguage)
        .orElse(null)
    mergedTranslation.assert.isNotNull()
    mergedTranslation!!.text.assert.isEqualTo(germanValue)
  }

  @Test
  fun `apply merge - renames the target key when the source key was renamed`() {
    keyService.edit(keyService.get(testData.featureKeyToUpdate.id), "renamed-key", null, testData.featureBranch.name)
    val merge = dryRunFeatureBranchMerge()
    merge.changes
      .single()
      .change.assert
      .isEqualTo(BranchKeyMergeChangeType.UPDATE)
    branchService.applyMerge(testData.project.id, merge.id, false)
    testData.mainKeyToUpdate
      .refresh()
      .name.assert
      .isEqualTo("renamed-key")
    dryRunFeatureBranchMerge().changes.assert.isEmpty()
  }

  @Test
  fun `dry-run marks a key renamed differently on both sides as conflict`() {
    keyService.edit(keyService.get(testData.featureKeyToUpdate.id), "feature-name", null, testData.featureBranch.name)
    keyService.edit(keyService.get(testData.mainKeyToUpdate.id), "main-name", null, testData.mainBranch.name)
    dryRunFeatureBranchMerge()
      .changes
      .single()
      .change.assert
      .isEqualTo(BranchKeyMergeChangeType.CONFLICT)
  }

  @Test
  fun `apply merge - rename onto an existing target name fails and rolls back`() {
    val clashingName = "rename-clash-name"
    createKey(clashingName, "x", testData.mainBranch)
    keyService.edit(keyService.get(testData.featureKeyToUpdate.id), clashingName, null, testData.featureBranch.name)
    val merge = dryRunFeatureBranchMerge()
    assertThrows<ValidationException> { branchService.applyMerge(testData.project.id, merge.id, false) }
    testData.mainKeyToUpdate
      .refresh()
      .name.assert
      .isEqualTo(BranchMergeTestData.UPDATE_KEY_NAME)
  }

  @Test
  fun `apply merge - copies maxCharLimit of an added key and leaves no phantom change`() {
    val featureKey = createKey(additionKeyName, additionValue, testData.featureBranch)
    featureKey.maxCharLimit = 42
    keyRepository.save(featureKey)
    branchService.applyMerge(testData.project.id, dryRunFeatureBranchMerge().id, false)
    testData.mainBranch
      .findKey(additionKeyName)!!
      .maxCharLimit.assert
      .isEqualTo(42)
    dryRunFeatureBranchMerge().changes.assert.isEmpty()
  }

  @Test
  fun `apply merge - same-name key added on both branches resolved as source takes the source value`() {
    val mainKey = createKey("same-name-new-key", "Main value", testData.mainBranch)
    createKey("same-name-new-key", "Feature value", testData.featureBranch)
    val merge = dryRunFeatureBranchMerge()
    resolveOnlyConflict(merge, BranchKeyMergeResolutionType.SOURCE)
    branchService.applyMerge(testData.project.id, merge.id, false)
    mainKey.enTranslation().assert.isEqualTo("Feature value")
    dryRunFeatureBranchMerge().changes.assert.isEmpty()
  }

  @Test
  fun `apply merge - same-name key added on both branches resolved as target keeps the target value`() {
    val mainKey = createKey("same-name-new-key", "Main value", testData.mainBranch)
    val featureKey = createKey("same-name-new-key", "Feature value", testData.featureBranch)
    addTagToKey(featureKey, "feature-only-tag")
    val merge = dryRunFeatureBranchMerge()
    resolveOnlyConflict(merge, BranchKeyMergeResolutionType.TARGET)
    branchService.applyMerge(testData.project.id, merge.id, false)
    mainKey.enTranslation().assert.isEqualTo("Main value")
    val mainTags = mainKey.refresh().keyMeta?.tags
    mainTags.orEmpty().assert.isEmpty()
    dryRunFeatureBranchMerge().changes.assert.isEmpty()
  }

  @Test
  fun `apply merge - updates a target key that was deleted and re-created with the same name`() {
    keyService.hardDelete(testData.mainKeyToDelete.id)
    val recreated = createKey(BranchMergeTestData.DELETE_KEY_NAME, "Text to delete", testData.mainBranch)
    updateKeyTranslation(testData.featureKeyToDelete, updatedValue)
    val merge = dryRunFeatureBranchMerge()
    merge.changes
      .single()
      .change.assert
      .isEqualTo(BranchKeyMergeChangeType.UPDATE)
    branchService.applyMerge(testData.project.id, merge.id, false)
    recreated.enTranslation().assert.isEqualTo(updatedValue)
    dryRunFeatureBranchMerge().changes.assert.isEmpty()
  }

  @Test
  fun `apply merge - deletes a key before renaming another key to its name`() {
    deleteFeatureKey()
    keyService.edit(
      keyService.get(testData.featureKeyToUpdate.id),
      BranchMergeTestData.DELETE_KEY_NAME,
      null,
      testData.featureBranch.name,
    )
    val merge = dryRunFeatureBranchMerge()
    merge.changes
      .map { it.change }
      .assert
      .containsExactlyInAnyOrder(BranchKeyMergeChangeType.DELETE, BranchKeyMergeChangeType.UPDATE)
    branchService.applyMerge(testData.project.id, merge.id, false)
    testData.mainBranch
      .findKey(BranchMergeTestData.DELETE_KEY_NAME)!!
      .id.assert
      .isEqualTo(testData.mainKeyToUpdate.id)
    keyRepository
      .findById(testData.mainKeyToDelete.id)
      .orElseThrow()
      .deletedAt.assert
      .isNotNull()
    dryRunFeatureBranchMerge().changes.assert.isEmpty()
  }

  @Test
  fun `dry-run offers a new key that reuses the old name of a renamed key as addition`() {
    keyService.edit(keyService.get(testData.featureKeyToUpdate.id), "renamed-key", null, testData.featureBranch.name)
    createKey(BranchMergeTestData.UPDATE_KEY_NAME, "Reused name", testData.featureBranch)
    dryRunFeatureBranchMerge()
      .changes
      .map { it.change }
      .assert
      .containsExactlyInAnyOrder(BranchKeyMergeChangeType.UPDATE, BranchKeyMergeChangeType.ADD)
  }

  @Test
  fun `dry-run does not pair a key deleted on the target with another target key renamed to its name`() {
    deleteTargetKey()
    keyService.edit(
      keyService.get(testData.mainKeyToUpdate.id),
      BranchMergeTestData.DELETE_KEY_NAME,
      null,
      testData.mainBranch.name,
    )
    dryRunFeatureBranchMerge().changes.assert.isEmpty()
  }

  @Test
  fun `apply merge - update from origin renames the child key`() {
    keyService.edit(keyService.get(testData.mainKeyToUpdate.id), "renamed-on-main", null, testData.mainBranch.name)
    val merge = dryRunUpdateFromMain()
    merge.changes
      .single()
      .change.assert
      .isEqualTo(BranchKeyMergeChangeType.UPDATE)
    branchService.applyMerge(testData.project.id, merge.id, false)
    testData.featureKeyToUpdate
      .refresh()
      .name.assert
      .isEqualTo("renamed-on-main")
    dryRunUpdateFromMain().changes.assert.isEmpty()
  }

  @Test
  fun `apply merge - moves the target key to the namespace of the source key`() {
    moveFeatureKeyToNamespace("ns")
    branchService.applyMerge(testData.project.id, dryRunFeatureBranchMerge().id, false)
    executeInNewTransaction {
      keyRepository
        .findByIdOrNull(testData.mainKeyToUpdate.id)!!
        .namespace
        ?.name.assert
        .isEqualTo("ns")
    }
  }

  @Test
  fun `apply merge - adds a new key that reuses the old name of a renamed key`() {
    keyService.edit(keyService.get(testData.featureKeyToUpdate.id), "renamed-key", null, testData.featureBranch.name)
    createKey(BranchMergeTestData.UPDATE_KEY_NAME, "Reused name", testData.featureBranch)
    branchService.applyMerge(testData.project.id, dryRunFeatureBranchMerge().id, false)
    testData.mainKeyToUpdate
      .refresh()
      .name.assert
      .isEqualTo("renamed-key")
    testData.mainBranch
      .findKey(BranchMergeTestData.UPDATE_KEY_NAME)!!
      .enTranslation()
      .assert
      .isEqualTo("Reused name")
    dryRunFeatureBranchMerge().changes.assert.isEmpty()
  }

  private fun setLabels(
    translation: Translation,
    vararg labels: Label,
  ) {
    translation.labels.clear()
    labels.forEach { translation.addLabel(it) }
    translationService.save(translation)
  }

  private fun prepareMergeScenario(): BranchMerge {
    createFeatureOnlyKey()
    deleteFeatureKey()
    updateFeatureKey()
    waitForNotThrowing(timeout = 5000, pollTime = 100) {
      testData.featureBranch
        .refresh()!!
        .revision.assert
        .isEqualTo(4)
    }
    return dryRunFeatureBranchMerge()
  }

  private fun resolveOnlyConflict(
    merge: BranchMerge,
    resolution: BranchKeyMergeResolutionType,
  ) {
    val conflict = merge.changes.single { it.change == BranchKeyMergeChangeType.CONFLICT }
    branchService.resolveConflict(
      testData.project.id,
      merge.id,
      ResolveBranchMergeConflictRequest(conflict.id, resolution),
    )
  }

  private fun dryRunFeatureBranchMerge(): BranchMerge {
    return branchService.dryRunMerge(
      testData.featureBranch.refresh()!!,
      testData.mainBranch.refresh()!!,
    )
  }

  private fun dryRunUpdateFromMain() =
    branchService.dryRunMerge(testData.mainBranch.refresh()!!, testData.featureBranch.refresh()!!)

  private fun applyMerge(merge: BranchMerge) = branchService.applyMerge(testData.project.id, merge.id)

  private fun dryRunAndMergeFeatureBranch() {
    val merge = dryRunFeatureBranchMerge()
    applyMerge(merge)
  }

  private fun createFeatureOnlyKey() {
    createKey(additionKeyName, additionValue, testData.featureBranch)
  }

  private fun createKey(
    name: String,
    enTranslation: String,
    branch: Branch,
  ): Key {
    val dto =
      CreateKeyDto(
        name = name,
        translations = mapOf("en" to enTranslation),
        branch = branch.name,
      )
    return keyService.create(testData.project, dto)
  }

  private fun deleteFeatureKey() {
    keyService.hardDelete(testData.featureKeyToDelete.id)
  }

  private fun updateFeatureKey() {
    updateKeyTranslation(testData.featureKeyToUpdate, updatedValue)
  }

  private fun prepareFeatureKeyLanguageReplacement(
    tag: String,
    name: String,
    value: String,
  ) {
    val language = addProjectLanguage(tag, name)
    replaceTranslationLanguage(testData.featureKeyToUpdate, testData.englishLanguage, language, value)
  }

  private fun addProjectLanguage(
    tag: String,
    name: String,
  ): Language {
    return languageService.createLanguage(
      LanguageRequest(
        name = name,
        originalName = name,
        tag = tag,
      ),
      testData.project,
    )
  }

  private fun replaceTranslationLanguage(
    key: Key,
    fromLanguage: Language,
    toLanguage: Language,
    value: String,
  ) {
    val managedKey = keyService.get(key.id)
    translationService
      .find(managedKey, fromLanguage)
      .orElse(null)
      ?.let { translationService.deleteByIdIn(listOf(it.id)) }
    val translation = translationService.getOrCreate(managedKey, toLanguage)
    translationService.setTranslationText(translation, value)
  }

  private fun reAddFeatureDeletedKey() {
    keyService.hardDelete(testData.featureKeyToDelete.id)
    val dto =
      CreateKeyDto(
        name = BranchMergeTestData.DELETE_KEY_NAME,
        translations = mapOf("en" to "Re-added feature value"),
        branch = testData.featureBranch.name,
      )
    keyService.create(testData.project, dto)
  }

  private fun removeTagFromKey(
    key: Key,
    tag: Tag,
  ) {
    tagService.removeKeyTag(key, tag.id)
  }

  private fun addTagToKey(
    key: Key,
    tagName: String,
  ) {
    val key = keyService.getKeysWithTagsById(key.project.id, listOf(key.id)).singleOrNull() ?: throw NotFoundException()
    tagService.tagKey(key, tagName)
  }

  private fun updateKeyTranslation(
    key: Key,
    value: String,
  ) {
    val managedKey = keyService.get(key.id)
    val translation = translationService.getOrCreate(managedKey, testData.englishLanguage)
    translationService.setTranslationText(translation, value)
  }

  private fun getTranslation(
    key: Key,
    language: Language,
  ): Translation = translationService.getTranslationsWithLabels(listOf(key.id), listOf(language.id)).single()

  private fun getScreenshotReferences(keyId: Long): List<KeyScreenshotReference> =
    keyScreenshotReferenceRepository.getAllByKeyIdIn(listOf(keyId))

  private fun addScreenshotReference(key: Key): Screenshot {
    val screenshot = screenshotRepository.save(Screenshot())
    val reference =
      KeyScreenshotReference().apply {
        this.key = keyService.get(key.id)
        this.screenshot = screenshot
        positions = mutableListOf()
        originalText = "Screenshot text"
      }
    keyScreenshotReferenceRepository.save(reference)
    return screenshot
  }

  private fun Branch.findKey(name: String): Key? {
    return keyRepository.findAllByBranchId(this.id).firstOrNull { it.name == name && it.deletedAt == null }
  }

  private fun Branch.refresh(): Branch? {
    return branchRepository.findById(this.id).orElse(null)
  }

  private fun Key.refresh(): Key {
    return keyRepository.findOneWithTags(this.id)
  }

  private fun Task.refresh(): Task {
    return taskRepository.findByIdOrNull(this.id) ?: throw IllegalStateException("Task not found")
  }

  private fun Key.enTranslation(): String? {
    val key = keyService.find(this.id) ?: return null
    return translationService.find(key, testData.englishLanguage).orElse(null)?.text
  }

  private fun deleteTargetKey() {
    keyService.hardDelete(testData.mainKeyToDelete.id)
  }

  private fun reCreateTargetKeyWithSameName(): String {
    val originalName = testData.mainKeyToDelete.name
    keyService.hardDelete(testData.mainKeyToDelete.id)
    createKey(originalName, "Re-created in target", testData.mainBranch)
    return originalName
  }

  private fun renameFeatureKey(newName: String) {
    val key = keyService.get(testData.featureKeyToUpdate.id)
    keyService.edit(
      key,
      newName,
      key.namespace?.name,
      null,
    )
  }

  private fun moveFeatureKeyToNamespace(namespaceName: String) {
    namespaceService.create(namespaceName, testData.project.id)
    val key = keyService.get(testData.featureKeyToUpdate.id)
    keyService.edit(
      key,
      key.name,
      namespaceName,
      null,
    )
  }

  private fun prepareNamespaceRecreation() {
    val namespaceName = "temp-namespace"
    val namespace1 = namespaceService.create(namespaceName, testData.project.id)!!

    createKey("namespace-test-key", "Test value", testData.featureBranch)

    branchSnapshotService.createInitialSnapshot(
      testData.project.id,
      testData.mainBranch,
      testData.featureBranch,
    )

    namespaceService.delete(namespace1)
    namespaceService.create(namespaceName, testData.project.id)
  }

  private fun addDifferentTranslationsForNewLanguage(
    tag: String,
    name: String,
    mainValue: String,
    featureValue: String,
  ) {
    val newLanguage = addProjectLanguage(tag, name)

    val mainKey = keyService.get(testData.mainKeyToUpdate.id)
    val mainTranslation = translationService.getOrCreate(mainKey, newLanguage)
    translationService.setTranslationText(mainTranslation, mainValue)

    val featureKey = keyService.get(testData.featureKeyToUpdate.id)
    val featureTranslation = translationService.getOrCreate(featureKey, newLanguage)
    translationService.setTranslationText(featureTranslation, featureValue)
  }

  private fun addScreenshotsToBothBranches() {
    addScreenshotReference(testData.mainKeyToUpdate)
    addScreenshotReference(testData.featureKeyToUpdate)
  }
}
