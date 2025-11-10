package io.tolgee.ee.service.branching

import io.tolgee.AbstractSpringTest
import io.tolgee.development.testDataBuilder.data.BranchMergeTestData
import io.tolgee.dtos.request.key.CreateKeyDto
import io.tolgee.ee.repository.branching.BranchRepository
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.model.branching.Branch
import io.tolgee.model.branching.BranchMerge
import io.tolgee.model.enums.BranchKeyMergeChangeType
import io.tolgee.model.enums.BranchKeyMergeResolutionType
import io.tolgee.model.key.Key
import io.tolgee.repository.KeyRepository
import io.tolgee.testing.assert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

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
      testData.featureBranch
    )
  }

  @Test
  fun `dry-run detects additions updates and deletions`() {
    val merge = prepareMergeScenario()

    val additions = merge.changes.filter { it.change == BranchKeyMergeChangeType.ADD }
    val deletions = merge.changes.filter { it.change == BranchKeyMergeChangeType.DELETE }
    val updates = merge.changes.filter { it.change == BranchKeyMergeChangeType.UPDATE }

    additions.assert.hasSize(1)
    additions.first().sourceKey!!.name.assert.isEqualTo(additionKeyName)
    additions.first().resolution.assert.isEqualTo(BranchKeyMergeResolutionType.SOURCE)

    deletions.assert.hasSize(1)
    deletions.first().targetKey!!.name.assert.isEqualTo(BranchMergeTestData.DELETE_KEY_NAME)

    updates.assert.hasSize(1)
    updates.first().sourceKey!!.name.assert.isEqualTo(BranchMergeTestData.UPDATE_KEY_NAME)
    updates.first().resolution.assert.isEqualTo(BranchKeyMergeResolutionType.SOURCE)
  }

  @Test
  fun `apply merge propagates additions updates and deletions`() {
    val merge = prepareMergeScenario()

    branchService.applyMerge(testData.project.id, merge.id)

    val additionInMain = testData.mainBranch.findKey(additionKeyName)
    additionInMain.assert.isNotNull()
    additionInMain!!.enTranslation().assert.isEqualTo(additionValue)

    testData.mainKeyToUpdate.enTranslation().assert.isEqualTo(updatedValue)
    keyService.find(testData.mainKeyToDelete.id).assert.isNull()
  }

  private fun prepareMergeScenario(): BranchMerge {
    createFeatureOnlyKey()
    deleteFeatureKey()
    updateFeatureKey()
    waitForNotThrowing(timeout = 2500, pollTime = 250) {
      testData.featureBranch.refresh()!!.revision.assert.isEqualTo(3)
    }

    return branchService.dryRunMerge(
      testData.featureBranch.refresh()!!,
      testData.mainBranch.refresh()!!,
    )
  }

  @Test
  fun `dry-run marks key as conflict when both branches change it`() {
    val mainConflictValue = "Main side conflict change"
    val featureConflictValue = "Feature side conflict change"

    updateKeyTranslation(testData.mainConflictKey, mainConflictValue)
    updateKeyTranslation(testData.featureConflictKey, featureConflictValue)

    val merge = branchService.dryRunMerge(
      testData.featureBranch.refresh()!!,
      testData.mainBranch.refresh()!!,
    )

    val conflicts = merge.changes.filter { it.change == BranchKeyMergeChangeType.CONFLICT }
    conflicts.assert.hasSize(1)
    conflicts.first().sourceKey!!.name.assert.isEqualTo(BranchMergeTestData.CONFLICT_KEY_NAME)
    conflicts.first().targetKey!!.name.assert.isEqualTo(BranchMergeTestData.CONFLICT_KEY_NAME)
    conflicts.first().resolution.assert.isNull()
  }

  private fun createFeatureOnlyKey() {
    val dto = CreateKeyDto(
      name = additionKeyName,
      translations = mapOf("en" to additionValue),
      branch = testData.featureBranch.name
    )
    keyService.create(testData.project, dto)
  }

  private fun deleteFeatureKey() {
    keyService.delete(testData.featureKeyToDelete.id)
  }

  private fun updateFeatureKey() {
    updateKeyTranslation(testData.featureKeyToUpdate, updatedValue)
  }

  private fun updateKeyTranslation(key: Key, value: String) {
    val managedKey = keyService.get(key.id)
    val translation = translationService.getOrCreate(managedKey, testData.englishLanguage)
    translationService.setTranslationText(translation, value)
  }

  private fun Branch.findKey(name: String): Key? {
    return keyRepository.findAllByBranchId(this.id).firstOrNull { it.name == name }
  }

  private fun Branch.refresh(): Branch? {
    return branchRepository.findById(this.id).orElse(null)
  }

  private fun Key.enTranslation(): String? {
    val key = keyService.find(this.id) ?: return null
    return translationService.find(key, testData.englishLanguage).orElse(null)?.text
  }
}
