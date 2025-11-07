package io.tolgee.ee.api.v2.controllers.branching

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.constants.Message
import io.tolgee.development.testDataBuilder.data.BranchTestData
import io.tolgee.dtos.request.branching.ResolveBranchMergeConflictRequest
import io.tolgee.ee.repository.branching.BranchMergeChangeRepository
import io.tolgee.ee.repository.branching.BranchMergeRepository
import io.tolgee.ee.repository.branching.BranchRepository
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andHasErrorMessage
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.isValidId
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
import io.tolgee.util.addDays
import io.tolgee.util.addMinutes
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import java.math.BigDecimal

@Suppress("SpringJavaInjectionPointsAutowiringInspection")
class BranchControllerTest : ProjectAuthControllerTest("/v2/projects/") {

  lateinit var testData: BranchTestData

  @Autowired
  lateinit var branchRepository: BranchRepository

  @Autowired
  lateinit var branchMergeRepository: BranchMergeRepository

  @Autowired
  lateinit var branchMergeChangeRepository: BranchMergeChangeRepository

  @BeforeEach
  fun setup() {
    testData = BranchTestData(currentDateProvider)
    projectSupplier = { testData.projectBuilder.self }
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
  }

  /**
   * Tests that the branches are returned in the correct order (active and latest first)
   */
  @Test
  @ProjectJWTAuthTestMethod
  fun `returns list of branches`() {
    performProjectAuthGet("branches").andAssertThatJson {
      node("page.totalElements").isNumber.isEqualTo(BigDecimal(2))
      node("_embedded.branches") {
        node("[0].name").isEqualTo("main")
        node("[0].active").isEqualTo(true)
        node("[1].name").isEqualTo("feature-branch")
        node("[1].active").isEqualTo(true)
      }
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `creates default branch on project without branches`() {
    testData.projectBuilder.self = testData.secondProject
    performProjectAuthGet("branches").andAssertThatJson {
      node("page.totalElements").isNumber.isEqualTo(BigDecimal(1))
      node("_embedded.branches") {
        node("[0].name").isEqualTo("main")
        node("[0].active").isEqualTo(true)
        node("[0].isDefault").isEqualTo(true)
      }
    }
    // a default branch should be created and all keys moved under this branch
    keyService.getAll(testData.secondProject.id).first().branch!!.id.let { it ->
      it.assert.isNotNull()
      branchRepository.findByIdOrNull(it)!!.let { branch ->
        branch.name.assert.isEqualTo(Branch.DEFAULT_BRANCH_NAME)
        branch.isDefault.assert.isTrue
        branch.isProtected.assert.isTrue
      }
    }
  }

  @Test()
  @ProjectJWTAuthTestMethod
  fun `creates branch`() {
    performProjectAuthPost(
      "branches",
      mapOf(
        "name" to "new-branch",
        "originBranchId" to testData.mainBranch.id,
      )
    ).andAssertThatJson {
      node("name").isEqualTo("new-branch")
      node("active").isEqualTo(true)
    }
    branchRepository.findByProjectIdAndName(testData.project.id, "new-branch").assert.isNotNull()
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `unable to create branch with name of existing branch`() {
    performProjectAuthPost(
      "branches",
      mapOf(
        "name" to "feature-branch",
        "originBranchId" to testData.mainBranch.id,
      )
    ).andIsBadRequest.andHasErrorMessage(Message.BRANCH_ALREADY_EXISTS)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `deletes branch`() {
    performProjectAuthDelete("branches/${testData.featureBranch.id}").andIsOk
    testData.featureBranch.refresh().archivedAt.assert.isNotNull()
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `cannot delete default branch`() {
    performProjectAuthDelete("branches/${testData.mainBranch.id}").andIsForbidden
    testData.mainBranch.refresh().archivedAt.assert.isNull()
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `lists branch merges`() {
    createConflictKeys()

    performProjectAuthGet("branches/merge")
      .andIsOk.andAssertThatJson {
        node("page.totalElements").isNumber.isEqualTo(BigDecimal(1))
        node("_embedded.branchMerges") {
          isArray.hasSize(1)
          node("[0]") {
            node("sourceBranch.name").isEqualTo("feature-branch")
            node("targetBranch.name").isEqualTo("main")
            node("keyAdditionsCount").isEqualTo(1)
            node("keyDeletionsCount").isEqualTo(0)
            node("keyModificationsCount").isEqualTo(0)
            node("keyUnresolvedConflictsCount").isEqualTo(0)
          }
        }
      }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `deletes branch merge`() {
    performProjectAuthDelete("branches/merge/${testData.featureBranchMerge.id}").andIsOk

    branchMergeRepository.findMerge(testData.project.id, testData.featureBranchMerge.id).assert.isNull()
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `dry-run merges feature branch into main`() {
    createConflictKeys()

    var mergeId: Int
    performProjectAuthPost(
      "branches/merge/preview",
      mapOf(
        "name" to "new-merge",
        "targetBranchId" to testData.mainBranch.id,
        "sourceBranchId" to testData.featureBranch.id
      )
    ).let {
      it.andIsOk.andAssertThatJson {
        node("id").isValidId
      }
      mergeId = it.andReturn().mapResponseTo<Map<String, Int>>()["id"]!!
    }

    // test merge stats
    performProjectAuthGet(
      "branches/merge/$mergeId/preview",
    ).andIsOk.andAssertThatJson {
      node("keyModificationsCount").isEqualTo(1)
      node("keyAdditionsCount").isEqualTo(1)
      node("keyDeletionsCount").isEqualTo(1)
      node("keyUnresolvedConflictsCount").isEqualTo(1)
    }

    // test conflicted keys data
    performProjectAuthGet(
      "branches/merge/$mergeId/conflicts",
    ).andIsOk.andAssertThatJson {
      node("page.totalElements").isNumber.isEqualTo(BigDecimal(1))
      node("_embedded.branchMergeConflicts") {
        isArray.hasSize(1)
        node("[0]") {
          node("resolution").isNull()
          node("sourceKey") {
            node("keyName").isEqualTo("conflict-key")
            node("keyId").isValidId
            node("keyDescription").isEqualTo("conflict feature key description")
            node("translations.en.text").isEqualTo("new translation")
          }
          node("targetKey") {
            node("keyName").isEqualTo("conflict-key")
            node("keyId").isValidId
            node("keyDescription").isEqualTo("conflict key description")
            node("translations.en.text").isEqualTo("old translation")
          }
        }
      }
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `resolves merge conflict`() {
    val keys = createConflictKeys()
    val change = createMergeWithConflict(keys.second, keys.first)
    val mergeId = change.branchMerge.id

    performProjectAuthGet("branches/merge/$mergeId/conflicts")
      .andIsOk.andAssertThatJson {
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
      )
    ).andIsOk

    performProjectAuthGet("branches/merge/$mergeId/conflicts")
      .andIsOk.andAssertThatJson {
        node("_embedded.branchMergeConflicts") {
          node("[0].resolution").isEqualTo("SOURCE")
        }
      }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `merges resolved feature branch into main`() {
    val keys = createConflictKeys()
    // wait for revision numbers of branches to increase after conflict keys are created
    waitForNotThrowing(timeout = 10000, pollTime = 250) {
      testData.featureBranch.refresh().revision.assert.isGreaterThan(15)
      testData.mainBranch.refresh().revision.assert.isGreaterThan(10)
    }
    val change = createMergeWithConflict(
      keys.second,
      keys.first,
      BranchKeyMergeResolutionType.SOURCE
    )
    val mergeId = change.branchMerge.id

    performProjectAuthPost("branches/merge/$mergeId/apply").andIsOk
    translationService.find(keys.first.translations.first().id)!!.text.assert.isEqualTo("new translation")
  }

  private fun createConflictKeys(): Pair<Key, Key> {
    val date = currentDateProvider.date

    fun createKey(
      name: String,
      branch: Branch,
      translation: String,
      description: String
    ) = testData.projectBuilder.addKey {
      this.name = name
      this.branch = branch
    }.build keyBuilder@{
      addTranslation("en", translation).build {
        this@keyBuilder.self.translations.add(self)
      }
      addMeta { this.description = description }
    }.self

    val toBeModifiedKey = createKey(
      name = "key-to-change",
      branch = testData.mainBranch,
      translation = "key-to-be-change-translation-when-merged",
      description = "Main key description to be updated by merge"
    )

    val toModifyKey = createKey(
      name = "key-to-change",
      branch = testData.featureBranch,
      translation = "key-to-change-translation",
      description = "Featured key description to update key in main branch"
    )

    val conflictKeyMain = createKey(
      name = "conflict-key",
      branch = testData.mainBranch,
      translation = "old translation",
      description = "conflict key description"
    )

    val conflictKeyFeature = createKey(
      name = "conflict-key",
      branch = testData.featureBranch,
      translation = "new translation",
      description = "conflict feature key description"
    )

    currentDateProvider.run {
      forcedDate = date.addDays(-1)
      keyService.save(toBeModifiedKey)
      forcedDate = date.addMinutes(1)
      keyService.save(toModifyKey)

      forcedDate = date.addMinutes(1)
      keyService.save(conflictKeyMain)
      translationService.save(conflictKeyMain.translations.first { it.language.tag == "en" })
      forcedDate = date.addMinutes(2)
      keyService.save(conflictKeyFeature)
      translationService.save(conflictKeyFeature.translations.first { it.language.tag == "en" })

      forcedDate = date
    }
    return Pair(conflictKeyMain, conflictKeyFeature)
  }

  private fun createMergeWithConflict(
    sourceKey: Key,
    targetKey: Key,
    resolutionType: BranchKeyMergeResolutionType? = null
  ): BranchMergeChange {
    lateinit var change: BranchMergeChange
    val sourceBranch = testData.featureBranch.refresh()
    val targetBranch = testData.mainBranch.refresh()
    val branchMerge = testData.projectBuilder.addBranchMerge {
      this.sourceBranch = sourceBranch
      this.targetBranch = targetBranch
      sourceRevision = sourceBranch.revision
      targetRevision = targetBranch.revision
    }.build {
      change = addChange {
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

  private fun Branch.refresh(): Branch {
    return branchRepository.findByIdOrNull(this.id)!!
  }
}
