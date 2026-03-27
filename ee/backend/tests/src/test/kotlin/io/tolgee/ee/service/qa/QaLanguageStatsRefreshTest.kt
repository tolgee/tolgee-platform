package io.tolgee.ee.service.qa

import io.tolgee.batch.JobCharacter
import io.tolgee.batch.data.BatchJobDto
import io.tolgee.batch.data.BatchJobType
import io.tolgee.batch.events.OnBatchJobCancelled
import io.tolgee.batch.events.OnBatchJobFailed
import io.tolgee.batch.events.OnBatchJobSucceeded
import io.tolgee.component.eventListeners.LanguageStatsListener
import io.tolgee.constants.Feature
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.ee.development.QaTestData
import io.tolgee.ee.utils.QaTestUtil
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.model.batch.BatchJobStatus
import io.tolgee.model.translation.Translation
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
  lateinit var qa: QaTestUtil

  @Autowired
  lateinit var languageStatsListener: LanguageStatsListener

  @Autowired
  lateinit var enabledFeaturesProvider: PublicEnabledFeaturesProvider

  lateinit var testData: QaTestData

  @BeforeEach
  fun setup() {
    enabledFeaturesProvider.forceEnabled = setOf(Feature.QA_CHECKS)
    testData = QaTestData()
    testDataService.saveTestData(testData.root)
    qa.testData = testData
    qa.saveDefaultQaConfig()
    userAccount = testData.user
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
    userAccount = null
    enabledFeaturesProvider.forceEnabled = null
  }

  @Test
  fun `refreshes language stats on QA batch job success`() {
    setStaleAndCacheStats()
    clearStaleFlag()

    languageStatsListener.onQaBatchJobSucceeded(
      OnBatchJobSucceeded(createBatchJobDto(BatchJobStatus.SUCCESS)),
    )

    assertStaleCountEquals(0)
  }

  @Test
  fun `refreshes language stats on QA batch job failure`() {
    setStaleAndCacheStats()
    clearStaleFlag()

    languageStatsListener.onQaBatchJobFailed(
      OnBatchJobFailed(createBatchJobDto(BatchJobStatus.FAILED), errorMessage = null),
    )

    assertStaleCountEquals(0)
  }

  @Test
  fun `refreshes language stats on QA batch job cancellation`() {
    setStaleAndCacheStats()
    clearStaleFlag()

    languageStatsListener.onQaBatchJobCancelled(
      OnBatchJobCancelled(createBatchJobDto(BatchJobStatus.CANCELLED)),
    )

    assertStaleCountEquals(0)
  }

  private fun setStaleAndCacheStats() {
    executeInNewTransaction(platformTransactionManager) {
      val translation = entityManager.find(Translation::class.java, testData.frTranslation.id)
      translation.qaChecksStale = true
      entityManager.persist(translation)
    }
    languageStatsService.refreshLanguageStats(testData.project.id)

    val stats = getLanguageStats("fr")
    assertThat(stats!!.qaChecksStaleCount).isGreaterThan(0)
  }

  private fun clearStaleFlag() {
    executeInNewTransaction(platformTransactionManager) {
      val translation = entityManager.find(Translation::class.java, testData.frTranslation.id)
      translation.qaChecksStale = false
      entityManager.persist(translation)
    }
  }

  private fun assertStaleCountEquals(expected: Long) {
    // Listener methods run async via @Async proxy, since we need to wait
    waitForNotThrowing(AssertionFailedError::class) {
      val stats = getLanguageStats("fr")
      assertThat(stats!!.qaChecksStaleCount).isEqualTo(expected)
    }
  }

  private fun getLanguageStats(languageTag: String) =
    executeInNewTransaction(platformTransactionManager) {
      val projectLanguages = languageService.getProjectLanguages(testData.project.id).associateBy { it.id }
      languageStatsService
        .getLanguageStats(projectId = testData.project.id, projectLanguages.keys, null)
        .find { projectLanguages[it.languageId]!!.tag == languageTag }
    }

  private fun createBatchJobDto(status: BatchJobStatus) =
    BatchJobDto(
      id = -1,
      projectId = testData.project.id,
      authorId = testData.user.id,
      target = emptyList(),
      totalItems = 0,
      totalChunks = 0,
      chunkSize = 10,
      status = status,
      type = BatchJobType.QA_CHECK,
      params = null,
      maxPerJobConcurrency = 1,
      jobCharacter = JobCharacter.SLOW,
      hidden = true,
      debouncingKey = null,
    )
}
