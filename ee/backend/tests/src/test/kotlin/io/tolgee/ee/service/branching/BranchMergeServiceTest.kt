package io.tolgee.ee.service.branching

import io.tolgee.AbstractSpringTest
import io.tolgee.development.testDataBuilder.data.BranchMergeTestData
import io.tolgee.dtos.request.LanguageRequest
import io.tolgee.dtos.request.key.CreateKeyDto
import io.tolgee.ee.repository.TaskRepository
import io.tolgee.ee.repository.branching.BranchRepository
import io.tolgee.ee.service.LabelServiceImpl
import io.tolgee.exceptions.NotFoundException
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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.repository.findByIdOrNull
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
class BranchMergeServiceTest : AbstractSpringTest() {
  @Autowired
  lateinit var branchService: BranchServiceImpl

  @Autowired
  lateinit var branchSnapshotService: BranchSnapshotService

  @Autowired
  lateinit var branchRepository: BranchRepository

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

  private lateinit var testData: BranchMergeTestData

  private val additionKeyName = "feature-only-key"
  private val additionValue = "Feature branch only value"
  private val updatedValue = "Feature branch updated text"

  @BeforeEach
  fun setup() {
    testData = BranchMergeTestData()
    testDataService.saveTestData(testData.root)
    branchSnapshotService.createInitialSnapshot(
      testData.project.id,
      testData.mainBranch,
      testData.featureBranch,
    )
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
  }

  @Test
  fun `apply merge - keeps branch and resets snapshot`() {
    val merge = prepareMergeScenario()
    branchService.applyMerge(testData.project.id, merge.id, false)

    val refreshedBranch = testData.featureBranch.refresh()!!
    refreshedBranch.archivedAt.assert.isNull()
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
  fun `apply merge - moves finished and cancels unfinished tasks when deleting branch`() {
    val merge = prepareMergeScenario()
    branchService.applyMerge(testData.project.id, merge.id, true)

    testData.featureOpenTask
      .refresh()
      .let { task ->
        task.state.assert.isEqualTo(TaskState.CANCELED)
        task.branch!!
          .id.assert
          .isEqualTo(testData.mainBranch.id)
        task.originBranch!!
          .id.assert
          .isEqualTo(testData.featureBranch.id)
      }
    testData.featureFinishedTask
      .refresh()
      .let { task ->
        task.state.assert.isEqualTo(TaskState.FINISHED)
        task.branch!!
          .id.assert
          .isEqualTo(testData.mainBranch.id)
        task.originBranch!!
          .id.assert
          .isEqualTo(testData.featureBranch.id)
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
    // remove one tag from each key
    removeTagFromKey(testData.mainKeyToUpdate, testData.tag1)
    removeTagFromKey(testData.featureKeyToUpdate, testData.tag2)
    // add one to the feature key
    addTagToKey(testData.featureKeyToUpdate, "xyz")

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
  @Transactional
  fun `apply merge - labels merge correctly`() {
    val mainTranslation = getTranslation(testData.mainKeyToUpdate, testData.englishLanguage)
    val featureTranslation = getTranslation(testData.featureKeyToUpdate, testData.englishLanguage)

    setLabels(mainTranslation, testData.label2, testData.label3)
    setLabels(featureTranslation, testData.label1, testData.label3, testData.label4)

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
    return dryRunFeatureBranchMerge()
  }

  private fun dryRunFeatureBranchMerge(): BranchMerge {
    return branchService.dryRunMerge(
      testData.featureBranch.refresh()!!,
      testData.mainBranch.refresh()!!,
    )
  }

  private fun applyMerge(merge: BranchMerge) = branchService.applyMerge(testData.project.id, merge.id)

  private fun dryRunAndMergeFeatureBranch() {
    val merge = dryRunFeatureBranchMerge()
    applyMerge(merge)
  }

  private fun createFeatureOnlyKey() {
    val dto =
      CreateKeyDto(
        name = additionKeyName,
        translations = mapOf("en" to additionValue),
        branch = testData.featureBranch.name,
      )
    keyService.create(testData.project, dto)
  }

  private fun deleteFeatureKey() {
    keyService.delete(testData.featureKeyToDelete.id)
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
    keyService.delete(testData.featureKeyToDelete.id)
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
  ): Translation = translationService.find(key, language).orElse(null)!!

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
    return keyRepository.findAllByBranchId(this.id).firstOrNull { it.name == name }
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
}
