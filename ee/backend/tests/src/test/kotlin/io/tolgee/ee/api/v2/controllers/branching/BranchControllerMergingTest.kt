package io.tolgee.ee.api.v2.controllers.branching

import io.tolgee.ActivityTestUtil
import io.tolgee.ProjectAuthControllerTest
import io.tolgee.constants.Feature
import io.tolgee.constants.Message
import io.tolgee.development.testDataBuilder.data.BranchMergeTestData
import io.tolgee.dtos.request.branching.ResolveAllBranchMergeConflictsRequest
import io.tolgee.dtos.request.branching.ResolveBranchMergeConflictRequest
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.ee.repository.branching.BranchMergeChangeRepository
import io.tolgee.ee.repository.branching.BranchMergeRepository
import io.tolgee.ee.repository.branching.BranchRepository
import io.tolgee.ee.service.branching.BranchSnapshotService
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andHasErrorMessage
import io.tolgee.fixtures.andIsNotFound
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.mapResponseTo
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

  @Autowired
  lateinit var enabledFeaturesProvider: PublicEnabledFeaturesProvider

  @Autowired
  lateinit var activityTestUtil: ActivityTestUtil

  @BeforeEach
  fun setup() {
    testData = BranchMergeTestData()
    projectSupplier = { testData.projectBuilder.self }
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    enabledFeaturesProvider.forceEnabled = setOf(Feature.BRANCHING)
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
  fun `merge activity includes source and target branch names in params`() {
    val keys = initConflicts()
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

    waitForNotThrowing(timeout = 5000, pollTime = 250) {
      performProjectAuthGet("activity")
        .andIsOk
        .andAssertThatJson {
          node("_embedded.activities") {
            isArray.isNotEmpty
            node("[0].type").isEqualTo("BRANCH_MERGE")
            node("[0].params.source").isEqualTo("feature")
            node("[0].params.target").isEqualTo("main")
          }
        }
    }
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

  @Test
  @ProjectJWTAuthTestMethod
  fun `merge changes include merged key and changed translations`() {
    branchSnapshotService.createInitialSnapshot(
      testData.project.id,
      testData.mainBranch,
      testData.featureBranch,
    )
    updateKeyTranslation(testData.featureKeyToUpdate, "Updated text")

    val mergeId = createMergePreview(testData.featureBranch.id)

    performProjectAuthGet("branches/merge/$mergeId/changes?type=UPDATE")
      .andIsOk
      .andAssertThatJson {
        node("_embedded.branchMergeChanges") {
          isArray.hasSize(1)
          node("[0].sourceKey.keyName").isEqualTo(BranchMergeTestData.UPDATE_KEY_NAME)
          node("[0].mergedKey.translations.en.text").isEqualTo("Updated text")
          node("[0].changedTranslations").isArray.hasSize(1)
          node("[0].changedTranslations[0]").isEqualTo("en")
          node("[0].effectiveResolution").isEqualTo("SOURCE")
        }
      }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `conflicts expose merged key after resolve`() {
    initConflicts()
    val mergeId = createMergePreview(testData.featureBranch.id)
    val conflictId = getFirstConflictId(mergeId)

    performProjectAuthPut(
      "branches/merge/$mergeId/resolve",
      ResolveBranchMergeConflictRequest(
        changeId = conflictId,
        resolve = BranchKeyMergeResolutionType.SOURCE,
      ),
    ).andIsOk

    performProjectAuthGet("branches/merge/$mergeId/conflicts")
      .andIsOk
      .andAssertThatJson {
        node("_embedded.branchMergeConflicts") {
          node("[0].resolution").isEqualTo("SOURCE")
          node("[0].effectiveResolution").isEqualTo("SOURCE")
          node("[0].mergedKey.translations.en.text").isEqualTo("new translation")
          node("[0].changedTranslations").isArray.hasSize(1)
          node("[0].changedTranslations[0]").isEqualTo("en")
        }
      }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `identical values are not marked as changed`() {
    branchSnapshotService.createInitialSnapshot(
      testData.project.id,
      testData.mainBranch,
      testData.featureBranch,
    )
    updateKeyTranslation(testData.mainKeyToUpdate, "Same text")
    updateKeyTranslation(testData.featureKeyToUpdate, "Same text")

    val mergeId = createMergePreview(testData.featureBranch.id)

    performProjectAuthGet("branches/merge/$mergeId/changes?type=UPDATE")
      .andIsOk
      .andAssertThatJson {
        node("_embedded.branchMergeChanges") {
          isArray.hasSize(1)
          node("[0].sourceKey.keyName").isEqualTo(BranchMergeTestData.UPDATE_KEY_NAME)
          node("[0].changedTranslations").isArray.hasSize(0)
        }
      }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `merged merge conflicts are not accessible`() {
    val mergeId = testData.mergedConflictBranchMerge.id

    performProjectAuthGet("branches/merge/$mergeId/conflicts")
      .andIsNotFound
      .andHasErrorMessage(Message.BRANCH_MERGE_NOT_FOUND)
    performProjectAuthGet("branches/merge/$mergeId/changes")
      .andIsNotFound
      .andHasErrorMessage(Message.BRANCH_MERGE_NOT_FOUND)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `merged merge conflict resolution is not allowed`() {
    val mergeId = testData.mergedConflictBranchMerge.id
    val changeId = getConflictChangeId(mergeId)

    performProjectAuthPut(
      "branches/merge/$mergeId/resolve",
      ResolveBranchMergeConflictRequest(
        changeId = changeId,
        resolve = BranchKeyMergeResolutionType.SOURCE,
      ),
    ).andIsNotFound
      .andHasErrorMessage(Message.BRANCH_MERGE_CHANGE_NOT_FOUND)

    performProjectAuthPut(
      "branches/merge/$mergeId/resolve-all",
      ResolveAllBranchMergeConflictsRequest(
        resolve = BranchKeyMergeResolutionType.SOURCE,
      ),
    ).andIsNotFound
      .andHasErrorMessage(Message.BRANCH_MERGE_NOT_FOUND)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `merged merge preview and apply are not allowed`() {
    val mergeId = testData.mergedConflictBranchMerge.id

    performProjectAuthPost("branches/merge/$mergeId/refresh")
      .andIsNotFound
      .andHasErrorMessage(Message.BRANCH_MERGE_NOT_FOUND)
    performProjectAuthPost("branches/merge/$mergeId/apply")
      .andIsNotFound
      .andHasErrorMessage(Message.BRANCH_MERGE_NOT_FOUND)
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

  private fun createMergePreview(sourceBranchId: Long): Long {
    val response =
      performProjectAuthPost(
        "branches/merge/preview",
        mapOf(
          "sourceBranchId" to sourceBranchId,
        ),
      ).andIsOk
        .andReturn()
        .mapResponseTo<Map<String, Any>>()
    return (response["id"] as Number).toLong()
  }

  private fun getFirstConflictId(mergeId: Long): Long {
    val response =
      performProjectAuthGet("branches/merge/$mergeId/conflicts")
        .andIsOk
        .andReturn()
        .mapResponseTo<Map<String, Any>>()
    val embedded = response["_embedded"] as Map<*, *>
    val conflicts = embedded["branchMergeConflicts"] as List<Map<String, Any>>
    return (conflicts.first()["id"] as Number).toLong()
  }

  private fun getConflictChangeId(mergeId: Long): Long {
    return branchMergeChangeRepository
      .findAll()
      .first { it.branchMerge.id == mergeId && it.change == BranchKeyMergeChangeType.CONFLICT }
      .id
  }
}
