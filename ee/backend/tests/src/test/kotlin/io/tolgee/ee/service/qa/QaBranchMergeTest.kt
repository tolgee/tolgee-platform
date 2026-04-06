package io.tolgee.ee.service.qa

import com.posthog.server.PostHog
import io.tolgee.ProjectAuthControllerTest
import io.tolgee.batch.BatchJobService
import io.tolgee.batch.data.BatchJobType
import io.tolgee.constants.Feature
import io.tolgee.development.testDataBuilder.data.BranchMergeTestData
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.ee.repository.branching.BranchMergeChangeRepository
import io.tolgee.ee.repository.branching.BranchMergeRepository
import io.tolgee.ee.repository.branching.BranchRepository
import io.tolgee.ee.service.branching.BranchSnapshotService
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.model.branching.Branch
import io.tolgee.model.branching.BranchMergeChange
import io.tolgee.model.enums.BranchKeyMergeChangeType
import io.tolgee.model.enums.BranchKeyMergeResolutionType
import io.tolgee.model.enums.qa.QaCheckSeverity
import io.tolgee.model.enums.qa.QaCheckType
import io.tolgee.model.key.Key
import io.tolgee.model.qa.ProjectQaConfig
import io.tolgee.repository.qa.ProjectQaConfigRepository
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import io.tolgee.testing.assertions.Assertions.assertThat
import io.tolgee.util.executeInNewTransaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.repository.findByIdOrNull
import org.springframework.test.context.bean.override.mockito.MockitoBean

@SpringBootTest
@AutoConfigureMockMvc
@Suppress("SpringJavaInjectionPointsAutowiringInspection")
class QaBranchMergeTest : ProjectAuthControllerTest("/v2/projects/") {
  @MockitoBean
  @Autowired
  lateinit var postHog: PostHog

  @Autowired
  lateinit var enabledFeaturesProvider: PublicEnabledFeaturesProvider

  @Autowired
  lateinit var batchJobService: BatchJobService

  @Autowired
  lateinit var branchRepository: BranchRepository

  @Autowired
  lateinit var branchSnapshotService: BranchSnapshotService

  @Autowired
  lateinit var branchMergeRepository: BranchMergeRepository

  @Autowired
  lateinit var branchMergeChangeRepository: BranchMergeChangeRepository

  @Autowired
  lateinit var projectQaConfigRepository: ProjectQaConfigRepository

  lateinit var testData: BranchMergeTestData

  @BeforeEach
  fun setup() {
    enabledFeaturesProvider.forceEnabled = setOf(Feature.BRANCHING, Feature.QA_CHECKS)
    testData = BranchMergeTestData()
    testData.projectBuilder.self.useQaChecks = true
    projectSupplier = { testData.projectBuilder.self }
    testDataService.saveTestData(testData.root)
    userAccount = testData.user

    // Save QA config manually (QaTestUtil requires QaTestData, but this test uses BranchMergeTestData)
    executeInNewTransaction(platformTransactionManager) {
      projectQaConfigRepository.save(
        ProjectQaConfig(
          project = testData.projectBuilder.self,
          settings =
            QaCheckType.entries
              .associateWith { type ->
                when (type) {
                  QaCheckType.SPELLING, QaCheckType.GRAMMAR -> QaCheckSeverity.OFF
                  else -> QaCheckSeverity.WARNING
                }
              }.toMutableMap(),
        ),
      )
    }
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
    userAccount = null
    enabledFeaturesProvider.forceEnabled = null
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `branch merge triggers QA checks on modified translations`() {
    val keys = initConflicts()

    // Wait for branch revisions to be ready
    waitForNotThrowing(timeout = 10_000, pollTime = 250) {
      testData.featureBranch
        .refresh()
        .revision.assert
        .isGreaterThan(0)
      testData.mainBranch
        .refresh()
        .revision.assert
        .isGreaterThan(0)
    }

    // Create merge with conflict resolved as SOURCE (feature branch wins)
    val change =
      createMergeWithConflict(
        sourceKey = keys.second,
        targetKey = keys.first,
        resolutionType = BranchKeyMergeResolutionType.SOURCE,
      )
    val mergeId = change.branchMerge.id

    // Apply the merge — this triggers @RequestActivity(ActivityType.BRANCH_MERGE)
    performProjectAuthPost("branches/merge/$mergeId/apply").andIsOk

    // QA batch job should be created for the merged translations
    waitForNotThrowing(timeout = 30_000, pollTime = 500) {
      executeInNewTransaction(platformTransactionManager) {
        val jobs = batchJobService.getAllByProjectId(testData.projectBuilder.self.id)
        val qaJobs = jobs.filter { it.type == BatchJobType.QA_CHECK }
        qaJobs.assert.isNotEmpty
      }
    }
  }

  private fun Branch.refresh(): Branch {
    return branchRepository.findByIdOrNull(this.id)!!
  }

  private fun initConflicts(): Pair<Key, Key> {
    val keys = createConflictKeys()
    branchSnapshotService.createInitialSnapshot(
      testData.projectBuilder.self.id,
      testData.mainBranch,
      testData.featureBranch,
    )
    waitForNotThrowing(timeout = 10_000, pollTime = 250) {
      testData.featureBranch
        .refresh()
        .pending.assert.isFalse
    }
    updateKeyTranslation(keys.first, "main translation")
    updateKeyTranslation(keys.second, "new translation")
    return keys
  }

  private fun createConflictKeys(): Pair<Key, Key> {
    fun createKey(
      name: String,
      branch: Branch,
      translation: String,
    ) = testData.projectBuilder
      .addKey {
        this.name = name
        this.branch = branch
      }.build keyBuilder@{
        addTranslation("en", translation).build {
          this@keyBuilder.self.translations.add(self)
        }
        addMeta { this.description = "test description" }
      }.self

    val conflictKeyMain =
      createKey(
        name = "qa-merge-key",
        branch = testData.mainBranch,
        translation = "old translation",
      )

    val conflictKeyFeature =
      createKey(
        name = "qa-merge-key",
        branch = testData.featureBranch,
        translation = "old translation",
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
}
