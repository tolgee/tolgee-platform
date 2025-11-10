package io.tolgee.ee.api.v2.controllers.branching

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.constants.Message
import io.tolgee.development.testDataBuilder.data.BranchTestData
import io.tolgee.dtos.request.branching.ResolveBranchMergeConflictRequest
import io.tolgee.ee.repository.branching.BranchMergeChangeRepository
import io.tolgee.ee.repository.branching.BranchMergeRepository
import io.tolgee.ee.repository.branching.BranchRepository
import io.tolgee.ee.service.branching.BranchSnapshotService
import io.tolgee.ee.service.branching.DefaultBranchCreator
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andHasErrorMessage
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.isValidId
import io.tolgee.fixtures.mapResponseTo
import io.tolgee.fixtures.node
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.model.Project
import io.tolgee.model.branching.Branch
import io.tolgee.model.branching.BranchMergeChange
import io.tolgee.model.enums.BranchKeyMergeChangeType
import io.tolgee.model.enums.BranchKeyMergeResolutionType
import io.tolgee.model.key.Key
import io.tolgee.service.branching.BranchService
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
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

  @Autowired
  lateinit var branchSnapshotService: BranchSnapshotService

  @Autowired
  lateinit var branchService: BranchService

  @Autowired
  lateinit var defaultBranchCreator: DefaultBranchCreator

  @BeforeEach
  fun setup() {
    testData = BranchTestData(currentDateProvider)
    projectSupplier = { testData.projectBuilder.self }
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `returns list of all branches`() {
    performProjectAuthGet("branches").andAssertThatJson {
      node("page.totalElements").isNumber.isEqualTo(BigDecimal(4))
      node("_embedded.branches") {
        node("[0].name").isEqualTo("main")
        node("[0].active").isEqualTo(true)
      }
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `returns list of all branches sorted by created date`() {
    testData.projectBuilder.self = testData.secondProject
    createBranchesOneByObe(testData.secondProject)
    performProjectAuthGet("branches").andAssertThatJson {
      node("page.totalElements").isNumber.isEqualTo(BigDecimal(4))
      node("_embedded.branches") {
        node("[0].name").isEqualTo("main")
        node("[0].active").isEqualTo(true)
        node("[1].name").isEqualTo("third-branch")
        node("[1].active").isEqualTo(true)
        node("[2].name").isEqualTo("second-branch")
        node("[2].active").isEqualTo(true)
        node("[3].name").isEqualTo("first-branch")
        node("[3].active").isEqualTo(true)
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
    branchRepository.findActiveByProjectIdAndName(testData.project.id, "new-branch").assert.isNotNull()
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
    performProjectAuthDelete("branches/${testData.mergeBranch.id}").andIsOk
    testData.mergeBranch.refresh().let {
      it.archivedAt.assert.isNotNull()
      it.deletedAt.assert.isNotNull()
    }
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
            node("sourceBranch.name").isEqualTo("merge-branch")
            node("targetBranch.name").isEqualTo("main")
            node("keyAdditionsCount").isEqualTo(0)
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
    performProjectAuthDelete("branches/merge/${testData.mergedBranchMerge.id}").andIsOk

    branchMergeRepository.findMerge(testData.project.id, testData.mergedBranchMerge.id).assert.isNull()
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `dry-run merges feature branch into main`() {
    val keys = createConflictKeys()
    branchSnapshotService.createInitialSnapshot(
      testData.project.id,
      testData.mainBranch,
      testData.featureBranch
    )
    updateKeyTranslation(keys.first, "main translation")
    updateKeyTranslation(keys.second, "new translation")

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
      mergeId = it.andReturn().mapResponseTo<Map<String, Any>>()["id"]!! as Int
    }

    // test merge stats
    performProjectAuthGet(
      "branches/merge/$mergeId/preview",
    ).andIsOk.andAssertThatJson {
      node("keyModificationsCount").isEqualTo(0)
      node("keyAdditionsCount").isEqualTo(0)
      node("keyDeletionsCount").isEqualTo(0)
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
            node("translations.en.text").isEqualTo("main translation")
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
    branchSnapshotService.createInitialSnapshot(
      testData.project.id,
      testData.mainBranch,
      testData.featureBranch
    )
    updateKeyTranslation(keys.first, "main translation")
    updateKeyTranslation(keys.second, "new translation")
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

    val conflictKeyMain = createKey(
      name = "conflict-key",
      branch = testData.mainBranch,
      translation = "old translation",
      description = "conflict key description"
    )

    val conflictKeyFeature = createKey(
      name = "conflict-key",
      branch = testData.featureBranch,
      translation = "old translation",
      description = "conflict feature key description"
    )

    keyService.save(conflictKeyMain)
    translationService.save(conflictKeyMain.translations.first { it.language.tag == "en" })
    keyService.save(conflictKeyFeature)
    translationService.save(conflictKeyFeature.translations.first { it.language.tag == "en" })

    return Pair(conflictKeyMain, conflictKeyFeature)
  }

  private fun updateKeyTranslation(key: Key, value: String) {
    val managedKey = keyService.get(key.id)
    val translation = translationService.getOrCreate(managedKey, testData.englishLanguage)
    translationService.setTranslationText(translation, value)
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

  private fun createBranchesOneByObe(project: Project) {
    defaultBranchCreator.create(project.id)
    val refreshed = project.refresh()
    val defaultBranch = refreshed.getDefaultBranch()!!
    branchService.createBranch(refreshed.id, "first-branch", defaultBranch.id, testData.user)
    branchService.createBranch(refreshed.id, "second-branch", defaultBranch.id, testData.user)
    branchService.createBranch(refreshed.id, "third-branch", defaultBranch.id, testData.user)
  }

  private fun Branch.refresh(): Branch {
    return branchRepository.findByIdOrNull(this.id)!!
  }

  private fun Project.refresh(): Project {
    return projectRepository.findWithBranches(this.id)!!
  }
}
