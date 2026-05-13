package io.tolgee.ee.service.qa

import io.tolgee.batch.JobCharacter
import io.tolgee.batch.data.BatchJobDto
import io.tolgee.batch.data.BatchJobType
import io.tolgee.batch.data.BatchTranslationTargetItem
import io.tolgee.batch.events.OnBatchJobFinalized
import io.tolgee.component.eventListeners.LanguageStatsListener
import io.tolgee.constants.Feature
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.ee.development.QaLanguageStatsBranchTestData
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.model.batch.BatchJobStatus
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

  lateinit var testData: QaLanguageStatsBranchTestData

  @BeforeEach
  fun setup() {
    enabledFeaturesProvider.forceEnabled = setOf(Feature.QA_CHECKS, Feature.BRANCHING)
    testData = QaLanguageStatsBranchTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
    userAccount = null
    enabledFeaturesProvider.forceEnabled = null
  }

  @Test
  fun `refreshes language stats of main branch`() {
    cacheStaleStatsForBothBranches()
    clearStaleFlag(testData.mainFrTranslation.id)

    fireQaBatchJobFinalized(targetKeyIds = listOf(testData.mainKey.id))

    assertStaleCountEquals(branchId = testData.mainBranch.id, expected = 0)
  }

  @Test
  fun `refreshes language stats for all affected branches when the job spans multiple branches`() {
    cacheStaleStatsForBothBranches()
    clearStaleFlag(testData.mainFrTranslation.id)
    clearStaleFlag(testData.featureFrTranslation.id)

    fireQaBatchJobFinalized(
      targetKeyIds = listOf(testData.mainKey.id, testData.featureKey.id),
    )

    assertStaleCountEquals(branchId = testData.mainBranch.id, expected = 0)
    assertStaleCountEquals(branchId = testData.featureBranch.id, expected = 0)
  }

  private fun fireQaBatchJobFinalized(targetKeyIds: List<Long>) {
    val target =
      targetKeyIds.map { keyId ->
        BatchTranslationTargetItem(keyId = keyId, languageId = testData.frenchLanguage.id)
      }
    languageStatsListener.onQaBatchJobFinalized(
      OnBatchJobFinalized(
        job =
          createBatchJobDto(
            status = BatchJobStatus.SUCCESS,
            type = BatchJobType.QA_CHECK,
            target = target,
          ),
        activityRevisionId = -1L,
      ),
    )
  }

  /**
   * Translations in [QaLanguageStatsBranchTestData] are persisted with `qaChecksStale = true`,
   * so refreshing the language stats once caches a stale count of 1 per branch.
   */
  private fun cacheStaleStatsForBothBranches() {
    languageStatsService.refreshLanguageStats(testData.project.id, testData.mainBranch.id)
    languageStatsService.refreshLanguageStats(testData.project.id, testData.featureBranch.id)
    assertStaleCountEquals(branchId = testData.mainBranch.id, expected = 1)
    assertStaleCountEquals(branchId = testData.featureBranch.id, expected = 1)
  }

  /**
   * Use a JPQL bulk UPDATE so the change skips Hibernate's dirty tracking and never fires
   * an `OnProjectActivityEvent`.
   */
  private fun clearStaleFlag(translationId: Long) {
    executeInNewTransaction(platformTransactionManager) {
      entityManager
        .createQuery("UPDATE Translation t SET t.qaChecksStale = false WHERE t.id = :id")
        .setParameter("id", translationId)
        .executeUpdate()
    }
  }

  private fun assertStaleCountEquals(
    branchId: Long?,
    expected: Long,
  ) {
    // Listener methods may run async via the @Async proxy, so we need to wait.
    waitForNotThrowing(AssertionFailedError::class) {
      val stats = getLanguageStats(languageTag = "fr", branchId = branchId)
      assertThat(stats!!.qaChecksStaleCount).isEqualTo(expected)
    }
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

  private fun createBatchJobDto(
    status: BatchJobStatus,
    type: BatchJobType = BatchJobType.QA_CHECK,
    target: List<Any> = emptyList(),
  ) = BatchJobDto(
    id = -1,
    projectId = testData.project.id,
    authorId = testData.user.id,
    target = target,
    totalItems = target.size,
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
