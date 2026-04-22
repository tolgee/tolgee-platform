package io.tolgee.ee.service.qa

import io.tolgee.batch.JobCharacter
import io.tolgee.batch.data.BatchJobDto
import io.tolgee.batch.data.BatchJobType
import io.tolgee.batch.events.OnBatchJobFinalized
import io.tolgee.component.eventListeners.LanguageStatsListener
import io.tolgee.constants.Feature
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.ee.development.QaLanguageStatsBranchTestData
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.model.activity.ActivityModifiedEntity
import io.tolgee.model.activity.ActivityRevision
import io.tolgee.model.batch.BatchJobStatus
import io.tolgee.model.translation.Translation
import io.tolgee.repository.qa.ProjectQaConfigRepository
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.assertions.Assertions.assertThat
import io.tolgee.util.executeInNewTransaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.opentest4j.AssertionFailedError
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
@AutoConfigureMockMvc
class QaLanguageStatsRefreshTest : AuthorizedControllerTest() {
  @Autowired
  lateinit var languageStatsListener: LanguageStatsListener

  @Autowired
  lateinit var enabledFeaturesProvider: PublicEnabledFeaturesProvider

  @Autowired
  lateinit var projectQaConfigRepository: ProjectQaConfigRepository

  lateinit var testData: QaLanguageStatsBranchTestData

  @BeforeEach
  fun setup() {
    enabledFeaturesProvider.forceEnabled = setOf(Feature.QA_CHECKS, Feature.BRANCHING)
    testData = QaLanguageStatsBranchTestData()
    testDataService.saveTestData(testData.root)
    projectQaConfigRepository.save(testData.createDefaultQaConfig())
    userAccount = testData.user
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
    userAccount = null
    enabledFeaturesProvider.forceEnabled = null
  }

  @Test
  fun `refreshes language stats for default branch when revision only touches default branch`() {
    cacheStaleStatsForBothBranches()
    clearStaleFlag(testData.mainFrTranslation.id)

    val revisionId = seedActivityRevision(branchIds = listOf(null))

    fireBatchJobFinalized(revisionId)

    assertStaleCountEquals(branchId = null, expected = 0)
    // Feature branch must NOT have been refreshed — its stale count must stay above 0.
    assertStaleCountRemains(branchId = testData.featureBranch.id, minimum = 1)
  }

  @Test
  fun `refreshes language stats for non-default branch when revision only touches that branch`() {
    cacheStaleStatsForBothBranches()
    clearStaleFlag(testData.featureFrTranslation.id)

    val revisionId = seedActivityRevision(branchIds = listOf(testData.featureBranch.id))

    fireBatchJobFinalized(revisionId)

    assertStaleCountEquals(branchId = testData.featureBranch.id, expected = 0)
    // Default branch must NOT have been refreshed — its stale count must stay above 0.
    assertStaleCountRemains(branchId = null, minimum = 1)
  }

  @Test
  fun `refreshes language stats for all affected branches when revision spans multiple branches`() {
    cacheStaleStatsForBothBranches()
    clearStaleFlag(testData.mainFrTranslation.id)
    clearStaleFlag(testData.featureFrTranslation.id)

    val revisionId =
      seedActivityRevision(branchIds = listOf(null, testData.featureBranch.id))

    fireBatchJobFinalized(revisionId)

    assertStaleCountEquals(branchId = null, expected = 0)
    assertStaleCountEquals(branchId = testData.featureBranch.id, expected = 0)
  }

  @Test
  fun `does not refresh language stats for non-QA batch jobs`() {
    cacheStaleStatsForBothBranches()
    clearStaleFlag(testData.mainFrTranslation.id)

    val revisionId = seedActivityRevision(branchIds = listOf(null))

    languageStatsListener.onQaBatchJobFinalized(
      OnBatchJobFinalized(
        job = createBatchJobDto(BatchJobStatus.SUCCESS, type = BatchJobType.AUTO_TRANSLATE),
        activityRevisionId = revisionId,
      ),
    )

    // Not a QA_CHECK job — the stale count must not have been recomputed.
    assertStaleCountRemains(branchId = null, minimum = 1)
  }

  private fun fireBatchJobFinalized(revisionId: Long) {
    languageStatsListener.onQaBatchJobFinalized(
      OnBatchJobFinalized(
        job = createBatchJobDto(BatchJobStatus.SUCCESS),
        activityRevisionId = revisionId,
      ),
    )
  }

  /**
   * Translations in [QaLanguageStatsBranchTestData] are persisted with `qaChecksStale = true`,
   * so refreshing the language stats once caches a stale count of 1 per branch.
   */
  private fun cacheStaleStatsForBothBranches() {
    languageStatsService.refreshLanguageStats(testData.project.id, null)
    languageStatsService.refreshLanguageStats(testData.project.id, testData.featureBranch.id)
    assertStaleCountEquals(branchId = null, expected = 1)
    assertStaleCountEquals(branchId = testData.featureBranch.id, expected = 1)
  }

  private fun clearStaleFlag(translationId: Long) {
    executeInNewTransaction(platformTransactionManager) {
      val translation = entityManager.find(Translation::class.java, translationId)
      translation.qaChecksStale = false
      entityManager.persist(translation)
    }
  }

  private fun assertStaleCountEquals(
    branchId: Long?,
    expected: Long,
  ) {
    // Listener methods run async via @Async proxy, so we need to wait
    waitForNotThrowing(AssertionFailedError::class) {
      val stats = getLanguageStats(languageTag = "fr", branchId = branchId)
      assertThat(stats!!.qaChecksStaleCount).isEqualTo(expected)
    }
  }

  private fun assertStaleCountRemains(
    branchId: Long?,
    minimum: Long,
  ) {
    // The listener is @Async, so if it were going to (incorrectly) refresh this branch,
    // the drop could happen at any point during its execution window. Poll repeatedly
    // for the entire window and fail immediately if the count ever drops below `minimum`.
    val observationWindowMs = 1000L
    val pollIntervalMs = 50L
    val deadline = System.currentTimeMillis() + observationWindowMs
    do {
      val stats = getLanguageStats(languageTag = "fr", branchId = branchId)
      assertThat(stats!!.qaChecksStaleCount).isGreaterThanOrEqualTo(minimum)
      Thread.sleep(pollIntervalMs)
    } while (System.currentTimeMillis() < deadline)
  }

  private fun getLanguageStats(
    languageTag: String,
    branchId: Long?,
  ) = executeInNewTransaction(platformTransactionManager) {
    val projectLanguages = languageService.getProjectLanguages(testData.project.id).associateBy { it.id }
    languageStatsService
      .getLanguageStats(projectId = testData.project.id, projectLanguages.keys, branchId)
      .find { projectLanguages[it.languageId]!!.tag == languageTag }
  }

  private fun seedActivityRevision(branchIds: List<Long?>): Long =
    executeInNewTransaction(platformTransactionManager) {
      val revision =
        ActivityRevision().apply {
          projectId = testData.project.id
        }
      entityManager.persist(revision)
      branchIds.forEachIndexed { index, branchId ->
        val modified =
          ActivityModifiedEntity(
            activityRevision = revision,
            entityClass = "Translation",
            entityId = -(index.toLong() + 1),
          ).apply {
            this.branchId = branchId
          }
        entityManager.persist(modified)
      }
      entityManager.flush()
      revision.id
    }

  private fun createBatchJobDto(
    status: BatchJobStatus,
    type: BatchJobType = BatchJobType.QA_CHECK,
  ) = BatchJobDto(
    id = -1,
    projectId = testData.project.id,
    authorId = testData.user.id,
    target = emptyList(),
    totalItems = 0,
    totalChunks = 0,
    chunkSize = 10,
    status = status,
    type = type,
    params = null,
    maxPerJobConcurrency = 1,
    jobCharacter = JobCharacter.SLOW,
    hidden = true,
    debouncingKey = null,
  )
}
