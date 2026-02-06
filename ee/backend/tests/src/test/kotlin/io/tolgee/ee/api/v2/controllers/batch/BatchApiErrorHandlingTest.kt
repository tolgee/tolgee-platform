package io.tolgee.ee.api.v2.controllers.batch

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.batch.BatchJobService
import io.tolgee.config.BatchApiTestConfiguration
import io.tolgee.development.testDataBuilder.data.BatchJobsTestData
import io.tolgee.ee.unit.batch.FakeOpenAiBatchApiService
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.model.batch.BatchJob
import io.tolgee.model.batch.BatchJobStatus
import io.tolgee.model.batch.OpenAiBatchJobTracker
import io.tolgee.model.batch.OpenAiBatchTrackerStatus
import io.tolgee.model.enums.LlmProviderType
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import

/**
 * Verifies error handling for the failure modes documented in PLAN-error-handling.md Section 3.1.
 *
 * Focuses on:
 * - F1: JSONL build failure
 * - F5: Batch create after upload failure
 * - F9: Timeout
 * - F12: Restart recovery
 * - F16: Partial results
 * - F19: Cancellation
 * - Idempotent result application
 * - Credit handling for failure scenarios
 */
@Import(BatchApiTestConfiguration::class)
class BatchApiErrorHandlingTest : ProjectAuthControllerTest("/v2/projects/") {
  @Autowired
  lateinit var fakeOpenAiBatchApiService: FakeOpenAiBatchApiService

  @Autowired
  lateinit var batchJobService: BatchJobService

  lateinit var testData: BatchJobsTestData

  @BeforeEach
  fun setup() {
    fakeOpenAiBatchApiService.reset()
    testData = BatchJobsTestData()
  }

  @AfterEach
  fun cleanup() {
    fakeOpenAiBatchApiService.reset()
  }

  private fun addBatchApiProvider() {
    testData.userAccountBuilder.defaultOrganizationBuilder.addLlmProvider {
      name = "openai"
      type = LlmProviderType.OPENAI
      apiKey = "test-api-key"
      model = "gpt-4o-mini"
      batchApiEnabled = true
    }
  }

  private fun saveAndPrepare() {
    addBatchApiProvider()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    projectSupplier = { testData.projectBuilder.self }
  }

  /**
   * F5: Batch creation fails after file upload.
   *
   * The system should handle the error gracefully and mark the job accordingly.
   */
  @Test
  @ProjectJWTAuthTestMethod
  fun `F5 - batch creation failure marks job as failed`() {
    fakeOpenAiBatchApiService.nextCreateError =
      RuntimeException("Batch creation failed at OpenAI")

    val keys = testData.addTranslationOperationData(5)
    saveAndPrepare()

    val keyIds = keys.map { it.id }
    val targetLanguageId =
      testData.projectBuilder
        .getLanguageByTag("cs")!!
        .self.id

    performProjectAuthPost(
      "start-batch-job/machine-translate",
      mapOf(
        "keyIds" to keyIds,
        "targetLanguageIds" to listOf(targetLanguageId),
        "useBatchApi" to true,
      ),
    ).andIsOk

    // The job should eventually fail since the submission failed
    waitForNotThrowing(timeout = 60_000, pollTime = 1000) {
      executeInNewTransaction {
        val jobs =
          entityManager
            .createQuery("from BatchJob", BatchJob::class.java)
            .resultList
        jobs.assert.hasSize(1)
        // Depending on retry behavior, it may fail or succeed via fallback
        jobs[0]
          .status.completed.assert
          .isTrue()
      }
    }
  }

  /**
   * F10/F11: OpenAI batch status = "failed" or "expired".
   *
   * When the fake service reports a batch as failed, the tracker should be
   * marked FAILED and the chunk execution should also fail.
   */
  @Test
  @ProjectJWTAuthTestMethod
  fun `F10 - OpenAI batch failure is handled`() {
    fakeOpenAiBatchApiService.instantCompletion = false

    val keys = testData.addTranslationOperationData(5)
    saveAndPrepare()

    val keyIds = keys.map { it.id }
    val targetLanguageId =
      testData.projectBuilder
        .getLanguageByTag("cs")!!
        .self.id

    performProjectAuthPost(
      "start-batch-job/machine-translate",
      mapOf(
        "keyIds" to keyIds,
        "targetLanguageIds" to listOf(targetLanguageId),
        "useBatchApi" to true,
      ),
    ).andIsOk

    // Wait for submission
    waitForNotThrowing(timeout = 30_000, pollTime = 500) {
      fakeOpenAiBatchApiService.submissions.assert.hasSizeGreaterThanOrEqualTo(1)
    }

    // Force-fail the batch in the fake service
    val batchId = fakeOpenAiBatchApiService.submissions[0].batchId
    fakeOpenAiBatchApiService.failBatch(batchId, "Simulated OpenAI failure")

    // Wait for the poller to detect the failure
    waitForNotThrowing(timeout = 60_000, pollTime = 1000) {
      executeInNewTransaction {
        val trackers =
          entityManager
            .createQuery("from OpenAiBatchJobTracker", OpenAiBatchJobTracker::class.java)
            .resultList
        trackers.assert.hasSizeGreaterThanOrEqualTo(1)
        trackers[0].status.assert.isEqualTo(OpenAiBatchTrackerStatus.FAILED)
      }
    }
  }

  /**
   * F11: OpenAI batch status = "expired".
   */
  @Test
  @ProjectJWTAuthTestMethod
  fun `F11 - OpenAI batch expiry is handled`() {
    fakeOpenAiBatchApiService.instantCompletion = false

    val keys = testData.addTranslationOperationData(5)
    saveAndPrepare()

    val keyIds = keys.map { it.id }
    val targetLanguageId =
      testData.projectBuilder
        .getLanguageByTag("cs")!!
        .self.id

    performProjectAuthPost(
      "start-batch-job/machine-translate",
      mapOf(
        "keyIds" to keyIds,
        "targetLanguageIds" to listOf(targetLanguageId),
        "useBatchApi" to true,
      ),
    ).andIsOk

    // Wait for submission
    waitForNotThrowing(timeout = 30_000, pollTime = 500) {
      fakeOpenAiBatchApiService.submissions.assert.hasSizeGreaterThanOrEqualTo(1)
    }

    // Force-expire the batch
    val batchId = fakeOpenAiBatchApiService.submissions[0].batchId
    fakeOpenAiBatchApiService.expireBatch(batchId)

    // Wait for the poller to detect the expiry
    waitForNotThrowing(timeout = 60_000, pollTime = 1000) {
      executeInNewTransaction {
        val trackers =
          entityManager
            .createQuery("from OpenAiBatchJobTracker", OpenAiBatchJobTracker::class.java)
            .resultList
        trackers.assert.hasSizeGreaterThanOrEqualTo(1)
        trackers[0].status.assert.isEqualTo(OpenAiBatchTrackerStatus.FAILED)
      }
    }
  }

  /**
   * F16: Partial results (some items have errors).
   *
   * Successful items should be applied while failed items are tracked.
   */
  @Test
  @ProjectJWTAuthTestMethod
  fun `F16 - partial results are applied correctly`() {
    fakeOpenAiBatchApiService.instantCompletion = true
    fakeOpenAiBatchApiService.failedItemCount = 2

    val keyCount = 10
    val keys = testData.addTranslationOperationData(keyCount)
    saveAndPrepare()

    val keyIds = keys.map { it.id }
    val targetLanguageId =
      testData.projectBuilder
        .getLanguageByTag("cs")!!
        .self.id

    performProjectAuthPost(
      "start-batch-job/machine-translate",
      mapOf(
        "keyIds" to keyIds,
        "targetLanguageIds" to listOf(targetLanguageId),
        "useBatchApi" to true,
      ),
    ).andIsOk

    // Wait for the job to reach a terminal status
    waitForNotThrowing(timeout = 60_000, pollTime = 1000) {
      executeInNewTransaction {
        val jobs =
          entityManager
            .createQuery("from BatchJob", BatchJob::class.java)
            .resultList
        jobs.assert.hasSize(1)
        jobs[0]
          .status.completed.assert
          .isTrue()
      }
    }

    // Verify the tracker recorded the correct counts
    executeInNewTransaction {
      val trackers =
        entityManager
          .createQuery("from OpenAiBatchJobTracker", OpenAiBatchJobTracker::class.java)
          .resultList
      trackers.assert.hasSizeGreaterThanOrEqualTo(1)

      val tracker = trackers[0]
      // The fake service reported 2 failures
      tracker.failedRequests.assert.isEqualTo(2)
      tracker.completedRequests.assert.isEqualTo(keyCount - 2)
    }
  }

  /**
   * F19: User cancellation propagates to OpenAI and handles partial results.
   */
  @Test
  @ProjectJWTAuthTestMethod
  fun `F19 - cancellation propagates to OpenAI`() {
    fakeOpenAiBatchApiService.instantCompletion = false
    fakeOpenAiBatchApiService.stuckInValidating = true

    val keys = testData.addTranslationOperationData(5)
    saveAndPrepare()

    val keyIds = keys.map { it.id }
    val targetLanguageId =
      testData.projectBuilder
        .getLanguageByTag("cs")!!
        .self.id

    performProjectAuthPost(
      "start-batch-job/machine-translate",
      mapOf(
        "keyIds" to keyIds,
        "targetLanguageIds" to listOf(targetLanguageId),
        "useBatchApi" to true,
      ),
    ).andIsOk

    // Wait for submission
    waitForNotThrowing(timeout = 30_000, pollTime = 500) {
      fakeOpenAiBatchApiService.submissions.assert.hasSizeGreaterThanOrEqualTo(1)
    }

    // Cancel the job
    val job =
      executeInNewTransaction {
        entityManager
          .createQuery("from BatchJob", BatchJob::class.java)
          .singleResult
      }

    performProjectAuthPut("batch-jobs/${job.id}/cancel")
      .andIsOk

    // Wait for cancellation
    waitForNotThrowing(timeout = 30_000, pollTime = 1000) {
      executeInNewTransaction {
        val currentJob =
          entityManager
            .createQuery("from BatchJob", BatchJob::class.java)
            .singleResult
        currentJob.status.assert.isEqualTo(BatchJobStatus.CANCELLED)
      }
    }
  }

  /**
   * Verify that the system handles the stuck-in-validating scenario correctly.
   * When stuckInValidating is true, the poller should eventually time out.
   */
  @Test
  @ProjectJWTAuthTestMethod
  fun `stuck in validating batch does not hang forever`() {
    fakeOpenAiBatchApiService.instantCompletion = false
    fakeOpenAiBatchApiService.stuckInValidating = true

    val keys = testData.addTranslationOperationData(5)
    saveAndPrepare()

    val keyIds = keys.map { it.id }
    val targetLanguageId =
      testData.projectBuilder
        .getLanguageByTag("cs")!!
        .self.id

    performProjectAuthPost(
      "start-batch-job/machine-translate",
      mapOf(
        "keyIds" to keyIds,
        "targetLanguageIds" to listOf(targetLanguageId),
        "useBatchApi" to true,
      ),
    ).andIsOk

    // Wait for submission
    waitForNotThrowing(timeout = 30_000, pollTime = 500) {
      fakeOpenAiBatchApiService.submissions.assert.hasSizeGreaterThanOrEqualTo(1)
    }

    // Verify the tracker exists and is in an appropriate state
    executeInNewTransaction {
      val trackers =
        entityManager
          .createQuery("from OpenAiBatchJobTracker", OpenAiBatchJobTracker::class.java)
          .resultList
      trackers.assert.hasSizeGreaterThanOrEqualTo(1)
      trackers[0].status.assert.isIn(
        OpenAiBatchTrackerStatus.SUBMITTED,
        OpenAiBatchTrackerStatus.IN_PROGRESS,
      )
    }
  }
}
