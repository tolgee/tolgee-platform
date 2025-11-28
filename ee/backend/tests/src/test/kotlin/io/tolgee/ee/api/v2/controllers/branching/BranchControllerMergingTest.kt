package io.tolgee.ee.api.v2.controllers.branching

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.BranchMergeTestData
import io.tolgee.dtos.request.branching.ResolveBranchMergeConflictRequest
import io.tolgee.ee.repository.branching.BranchMergeChangeRepository
import io.tolgee.ee.repository.branching.BranchMergeRepository
import io.tolgee.ee.repository.branching.BranchRepository
import io.tolgee.ee.service.branching.BranchSnapshotService
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.node
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.model.branching.Branch
import io.tolgee.model.branching.BranchMergeChange
import io.tolgee.model.enums.BranchKeyMergeChangeType
import io.tolgee.model.enums.BranchKeyMergeResolutionType
import io.tolgee.model.key.Key
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull

@Suppress("SpringJavaInjectionPointsAutowiringInspection")
class BranchControllerMergingTest : ProjectAuthControllerTest("/v2/projects/") {
  lateinit var testData: BranchMergeTestData

  @Autowired
  lateinit var branchRepository: BranchRepository

  @Autowired
  lateinit var branchSnapshotService: BranchSnapshotService

  @Autowired
  lateinit var branchMergeRepository: BranchMergeRepository

  @Autowired
  lateinit var branchMergeChangeRepository: BranchMergeChangeRepository

  @BeforeEach
  fun setup() {
    testData = BranchMergeTestData()
    projectSupplier = { testData.projectBuilder.self }
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `resolves merge conflict`() {
    val keys = createConflictKeys()
    val change = createMergeWithConflict(keys.second, keys.first)
    val mergeId = change.branchMerge.id

    performProjectAuthGet("branches/merge/$mergeId/conflicts")
      .andIsOk
      .andAssertThatJson {
        node("_embedded.branchMergeConflicts") {
          node("[0].id").isEqualTo(change.id)
          node("[0].resolution").isNull()
        }
      }

    performProjectAuthPut(
      "branches/merge/$mergeId/resolve",
      ResolveBranchMergeConflictRequest(
        changeId = change.id,
        resolve = BranchKeyMergeResolutionType.SOURCE,
      ),
    ).andIsOk

    performProjectAuthGet("branches/merge/$mergeId/conflicts")
      .andIsOk
      .andAssertThatJson {
        node("_embedded.branchMergeConflicts") {
          node("[0].resolution").isEqualTo("SOURCE")
        }
      }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `merges resolved feature branch into main`() {
    val keys = initConflicts()
    // wait for revision numbers of branches to increase after conflict keys are created
    waitForNotThrowing(timeout = 10000, pollTime = 250) {
      testData.featureBranch
        .refresh()
        .revision.assert
        .isGreaterThan(0)
      testData.mainBranch
        .refresh()
        .revision.assert
        .isGreaterThan(0)
    }
    val change =
      createMergeWithConflict(
        keys.second,
        keys.first,
        BranchKeyMergeResolutionType.SOURCE,
      )
    val mergeId = change.branchMerge.id

    performProjectAuthPost("branches/merge/$mergeId/apply").andIsOk
    translationService
      .find(
        keys.first.translations
          .first()
          .id,
      )!!
      .text.assert
      .isEqualTo("new translation")
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `refreshes merge preview without losing resolved conflicts`() {
    branchSnapshotService.createInitialSnapshot(
      testData.project.id,
      testData.mainBranch,
      testData.conflictsBranch,
    )
    // we have to modify the same keys to
    updateKeyTranslation(testData.mainConflictKey, "main translation")
    updateKeyTranslation(testData.conflictsBranchKey, "new translation")

    performProjectAuthGet("branches/merge/${testData.conflictBranchMerge.id}/preview")
      .andIsOk
      .andAssertThatJson {
        node("outdated").isEqualTo(true)
        node("keyResolvedConflictsCount").isEqualTo(1)
      }

    performProjectAuthPost("branches/merge/${testData.conflictBranchMerge.id}/refresh")
      .andIsOk
      .andAssertThatJson {
        node("outdated").isEqualTo(false)
        node("keyResolvedConflictsCount").isEqualTo(1)
        node("keyUnresolvedConflictsCount").isEqualTo(0)
      }

    performProjectAuthGet("branches/merge/${testData.conflictBranchMerge.id}/conflicts")
      .andIsOk
      .andAssertThatJson {
        node("_embedded.branchMergeConflicts") {
          node("[0].resolution").isEqualTo("SOURCE")
        }
      }
  }

  private fun createMergeWithConflict(
    sourceKey: Key,
    targetKey: Key,
    resolutionType: BranchKeyMergeResolutionType? = null,
  ): BranchMergeChange {
    lateinit var change: BranchMergeChange
    val sourceBranch = testData.featureBranch.refresh()
    val targetBranch = testData.mainBranch.refresh()
    val branchMerge =
      testData.projectBuilder
        .addBranchMerge {
          this.sourceBranch = sourceBranch
          this.targetBranch = targetBranch
          sourceRevision = sourceBranch.revision
          targetRevision = targetBranch.revision
        }.build {
          change =
            addChange {
              this.change = BranchKeyMergeChangeType.CONFLICT
              this.sourceKey = sourceKey
              this.targetKey = targetKey
              this.resolution = resolutionType
              branchMerge.changes.add(this)
            }.self
        }.self
    branchMergeRepository.save(branchMerge)
    branchMergeChangeRepository.save(change)
    return change
  }

  private fun createConflictKeys(): Pair<Key, Key> {
    fun createKey(
      name: String,
      branch: Branch,
      translation: String,
      description: String,
    ) = testData.projectBuilder
      .addKey {
        this.name = name
        this.branch = branch
      }.build keyBuilder@{
        addTranslation("en", translation).build {
          this@keyBuilder.self.translations.add(self)
        }
        addMeta { this.description = description }
      }.self

    val conflictKeyMain =
      createKey(
        name = "conflict-key",
        branch = testData.mainBranch,
        translation = "old translation",
        description = "conflict key description",
      )

    val conflictKeyFeature =
      createKey(
        name = "conflict-key",
        branch = testData.featureBranch,
        translation = "old translation",
        description = "conflict feature key description",
      )

    keyService.save(conflictKeyMain)
    translationService.save(conflictKeyMain.translations.first { it.language.tag == "en" })
    keyService.save(conflictKeyFeature)
    translationService.save(conflictKeyFeature.translations.first { it.language.tag == "en" })

    return Pair(conflictKeyMain, conflictKeyFeature)
  }

  private fun updateKeyTranslation(
    key: Key,
    value: String,
  ) {
    val managedKey = keyService.get(key.id)
    val translation = translationService.getOrCreate(managedKey, testData.englishLanguage)
    translationService.setTranslationText(translation, value)
  }

  private fun initConflicts(): Pair<Key, Key> {
    val keys = createConflictKeys()
    branchSnapshotService.createInitialSnapshot(
      testData.project.id,
      testData.mainBranch,
      testData.featureBranch,
    )
    waitForNotThrowing(timeout = 10000, pollTime = 250) {
      testData.featureBranch
        .refresh()
        .pending.assert.isFalse
    }
    updateKeyTranslation(keys.first, "main translation")
    updateKeyTranslation(keys.second, "new translation")
    return keys
  }

  private fun Branch.refresh(): Branch {
    return branchRepository.findByIdOrNull(this.id)!!
  }
}
