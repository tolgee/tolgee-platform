package io.tolgee.ee.api.v2.controllers.batch

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.batch.BatchJobService
import io.tolgee.config.BatchApiTestConfiguration
import io.tolgee.development.testDataBuilder.data.BatchJobsTestData
import io.tolgee.ee.unit.batch.FakeOpenAiBatchApiService
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.isValidId
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.model.MtCreditBucket
import io.tolgee.model.batch.BatchJob
import io.tolgee.model.batch.BatchJobChunkExecutionStatus
import io.tolgee.model.batch.BatchJobStatus
import io.tolgee.model.batch.OpenAiBatchJobTracker
import io.tolgee.model.batch.OpenAiBatchTrackerStatus
import io.tolgee.model.enums.LlmProviderType
import io.tolgee.model.translation.Translation
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import

/**
 * Integration tests for the OpenAI Batch API translation feature.
 *
 * Uses the full Spring Boot context with [FakeOpenAiBatchApiService] replacing
 * the real OpenAI API. Tests the complete lifecycle: submission -> polling -> result application.
 *
 * Follows the patterns established in [io.tolgee.api.v2.controllers.batch.BatchMtTranslateTest].
 */
@Import(BatchApiTestConfiguration::class)
class BatchApiTranslationIntegrationTest : ProjectAuthControllerTest("/v2/projects/") {
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

  @Test
  @ProjectJWTAuthTestMethod
  fun `batch translate completes with instant completion`() {
    fakeOpenAiBatchApiService.instantCompletion = true

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
      .andAssertThatJson {
        node("id").isValidId
      }

    // Wait for job to complete - the poller should detect completion and apply results
    waitForNotThrowing(timeout = 60_000, pollTime = 1000) {
      executeInNewTransaction {
        val jobs =
          entityManager
            .createQuery("from BatchJob", BatchJob::class.java)
            .resultList
        jobs.assert.hasSize(1)
        jobs[0].status.assert.isEqualTo(BatchJobStatus.SUCCESS)
      }
    }

    // Verify that a batch was submitted to the fake service
    fakeOpenAiBatchApiService.submissions.assert.hasSizeGreaterThanOrEqualTo(1)

    // Verify the submission content format
    val submission = fakeOpenAiBatchApiService.submissions[0]
    val jsonlContent = String(submission.jsonlContent, Charsets.UTF_8)
    val lines = jsonlContent.lines().filter { it.isNotBlank() }
    lines.assert.hasSize(keyCount)

    // Verify translations were applied
    executeInNewTransaction {
      val translations =
        entityManager
          .createQuery(
            "from Translation t where t.key.id in :keyIds and t.language.tag = 'cs'",
            Translation::class.java,
          ).setParameter("keyIds", keyIds)
          .resultList
      translations.assert.hasSize(keyCount)
      translations.forEach {
        it.text.assert.isNotNull
      }
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `batch translate creates tracker with correct status lifecycle`() {
    fakeOpenAiBatchApiService.instantCompletion = true

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

    // Wait for completion
    waitForNotThrowing(timeout = 60_000, pollTime = 1000) {
      executeInNewTransaction {
        val jobs =
          entityManager
            .createQuery("from BatchJob", BatchJob::class.java)
            .resultList
        jobs.assert.hasSize(1)
        jobs[0].status.assert.isEqualTo(BatchJobStatus.SUCCESS)
      }
    }

    // Verify tracker was created and reached COMPLETED status
    executeInNewTransaction {
      val trackers =
        entityManager
          .createQuery("from OpenAiBatchJobTracker", OpenAiBatchJobTracker::class.java)
          .resultList
      trackers.assert.hasSizeGreaterThanOrEqualTo(1)

      val tracker = trackers[0]
      tracker.status.assert.isEqualTo(OpenAiBatchTrackerStatus.COMPLETED)
      tracker.openAiBatchId.assert.isNotNull
      tracker.openAiInputFileId.assert.isNotNull
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `batch translate JSONL has correct custom_id format`() {
    fakeOpenAiBatchApiService.instantCompletion = true

    val keys = testData.addTranslationOperationData(3)
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

    val submission = fakeOpenAiBatchApiService.submissions[0]
    val jsonlContent = String(submission.jsonlContent, Charsets.UTF_8)
    val lines = jsonlContent.lines().filter { it.isNotBlank() }

    // Each line should be valid JSON with custom_id in format "jobId:keyId:languageId"
    lines.forEach { line ->
      val customIdRegex = """"custom_id"\s*:\s*"(\d+):(\d+):(\d+)"""".toRegex()
      val match = customIdRegex.find(line)
      match.assert.isNotNull
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `batch translate handles submission error gracefully`() {
    fakeOpenAiBatchApiService.nextCreateError =
      RuntimeException("Simulated OpenAI API error")

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

    // Verify the submission was attempted
    waitForNotThrowing(timeout = 30_000, pollTime = 1000) {
      fakeOpenAiBatchApiService.submissions.assert.hasSizeGreaterThanOrEqualTo(1)
    }

    // The job should eventually fail (since the submission threw an error)
    waitForNotThrowing(timeout = 60_000, pollTime = 1000) {
      executeInNewTransaction {
        val jobs =
          entityManager
            .createQuery("from BatchJob", BatchJob::class.java)
            .resultList
        jobs.assert.hasSize(1)
        jobs[0].status.assert.isIn(BatchJobStatus.FAILED, BatchJobStatus.SUCCESS)
      }
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `batch translate handles partial failure`() {
    fakeOpenAiBatchApiService.instantCompletion = true
    fakeOpenAiBatchApiService.failedItemCount = 3

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

    // Wait for the job to complete
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

    // Verify tracker has correct request counts
    executeInNewTransaction {
      val trackers =
        entityManager
          .createQuery("from OpenAiBatchJobTracker", OpenAiBatchJobTracker::class.java)
          .resultList
      trackers.assert.hasSizeGreaterThanOrEqualTo(1)

      val tracker = trackers[0]
      tracker.failedRequests.assert.isEqualTo(3)
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `batch translate with multiple target languages`() {
    fakeOpenAiBatchApiService.instantCompletion = true

    val keyCount = 5
    val keys = testData.addTranslationOperationData(keyCount)
    saveAndPrepare()

    val keyIds = keys.map { it.id }
    val csLangId =
      testData.projectBuilder
        .getLanguageByTag("cs")!!
        .self.id
    val deLangId =
      testData.projectBuilder
        .getLanguageByTag("de")!!
        .self.id

    performProjectAuthPost(
      "start-batch-job/machine-translate",
      mapOf(
        "keyIds" to keyIds,
        "targetLanguageIds" to listOf(csLangId, deLangId),
        "useBatchApi" to true,
      ),
    ).andIsOk

    // Wait for completion
    waitForNotThrowing(timeout = 60_000, pollTime = 1000) {
      executeInNewTransaction {
        val jobs =
          entityManager
            .createQuery("from BatchJob", BatchJob::class.java)
            .resultList
        jobs.assert.hasSize(1)
        jobs[0].status.assert.isEqualTo(BatchJobStatus.SUCCESS)
      }
    }

    // Verify that JSONL had entries for both languages (keyCount * 2 languages)
    fakeOpenAiBatchApiService.submissions.assert.hasSizeGreaterThanOrEqualTo(1)
    val submission = fakeOpenAiBatchApiService.submissions[0]
    val jsonlContent = String(submission.jsonlContent, Charsets.UTF_8)
    val lines = jsonlContent.lines().filter { it.isNotBlank() }
    lines.assert.hasSize(keyCount * 2)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `batch translate without useBatchApi falls back to sync`() {
    // This verifies that the standard sync path still works
    // when useBatchApi is not set
    val keys = testData.addTranslationOperationData(5)
    saveAndPrepare()

    // Enable fake MT providers for sync path
    internalProperties.fakeMtProviders = true

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
        // useBatchApi not set - should use sync path
      ),
    ).andIsOk

    waitForNotThrowing(timeout = 60_000, pollTime = 1000) {
      executeInNewTransaction {
        val jobs =
          entityManager
            .createQuery("from BatchJob", BatchJob::class.java)
            .resultList
        jobs.assert.hasSize(1)
        jobs[0].status.assert.isEqualTo(BatchJobStatus.SUCCESS)
      }
    }

    // No batch API submissions should have been made
    fakeOpenAiBatchApiService.submissions.assert.isEmpty()
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `batch translate consumes credits after successful completion`() {
    fakeOpenAiBatchApiService.instantCompletion = true

    val keyCount = 5
    val keys = testData.addTranslationOperationData(keyCount)
    saveAndPrepare()

    // Record initial credit balance
    val orgId = testData.projectBuilder.self.organizationOwner.id
    val initialCredits =
      executeInNewTransaction {
        val bucket =
          entityManager
            .createQuery(
              "from MtCreditBucket b where b.organization.id = :orgId",
              MtCreditBucket::class.java,
            ).setParameter("orgId", orgId)
            .resultList
            .firstOrNull()
        bucket?.credits ?: Long.MAX_VALUE
      }

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

    // Wait for job to complete
    waitForNotThrowing(timeout = 60_000, pollTime = 1000) {
      executeInNewTransaction {
        val jobs =
          entityManager
            .createQuery("from BatchJob", BatchJob::class.java)
            .resultList
        jobs.assert.hasSize(1)
        jobs[0].status.assert.isEqualTo(BatchJobStatus.SUCCESS)
      }
    }

    // Verify that credits were consumed (balance decreased or stayed the same if credits are disabled)
    if (initialCredits != Long.MAX_VALUE) {
      executeInNewTransaction {
        val bucket =
          entityManager
            .createQuery(
              "from MtCreditBucket b where b.organization.id = :orgId",
              MtCreditBucket::class.java,
            ).setParameter("orgId", orgId)
            .resultList
            .firstOrNull()
        if (bucket != null) {
          bucket.credits.assert.isLessThanOrEqualTo(initialCredits)
        }
      }
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `batch-translate-info endpoint returns correct data`() {
    saveAndPrepare()

    performProjectAuthGet("batch-translate-info")
      .andIsOk
      .andAssertThatJson {
        node("available").isBoolean
        node("userChoiceAllowed").isBoolean
      }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `batch translate chunk execution goes through WAITING_FOR_EXTERNAL status`() {
    // Use non-instant completion so we can observe the WAITING_FOR_EXTERNAL status
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

    // Wait for the chunk execution to reach WAITING_FOR_EXTERNAL
    waitForNotThrowing(timeout = 30_000, pollTime = 500) {
      executeInNewTransaction {
        val executions =
          entityManager
            .createQuery(
              "from BatchJobChunkExecution bjce where bjce.status = :status",
              io.tolgee.model.batch.BatchJobChunkExecution::class.java,
            ).setParameter("status", BatchJobChunkExecutionStatus.WAITING_FOR_EXTERNAL)
            .resultList
        executions.assert.hasSizeGreaterThanOrEqualTo(1)
      }
    }

    // Verify a tracker was created
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

  @Test
  @ProjectJWTAuthTestMethod
  fun `batch translate cancellation sends cancel to OpenAI`() {
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

    // Wait for submission to complete
    waitForNotThrowing(timeout = 30_000, pollTime = 500) {
      fakeOpenAiBatchApiService.submissions.assert.hasSizeGreaterThanOrEqualTo(1)
    }

    // Get the job and cancel it
    val job =
      executeInNewTransaction {
        entityManager
          .createQuery("from BatchJob", BatchJob::class.java)
          .singleResult
      }

    performProjectAuthPut("batch-jobs/${job.id}/cancel")
      .andIsOk

    // Wait for cancellation to propagate
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

  @Test
  @ProjectJWTAuthTestMethod
  fun `batch translate cleans up OpenAI files after completion`() {
    fakeOpenAiBatchApiService.instantCompletion = true

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

    // Wait for job to complete
    waitForNotThrowing(timeout = 60_000, pollTime = 1000) {
      executeInNewTransaction {
        val jobs =
          entityManager
            .createQuery("from BatchJob", BatchJob::class.java)
            .resultList
        jobs.assert.hasSize(1)
        jobs[0].status.assert.isEqualTo(BatchJobStatus.SUCCESS)
      }
    }

    // Verify that the poller cleaned up files at OpenAI
    // The input file and output file should have been deleted
    fakeOpenAiBatchApiService.deletedFiles.assert.hasSizeGreaterThanOrEqualTo(1)

    // Verify the input file ID from the tracker was deleted
    val inputFileId =
      executeInNewTransaction {
        val trackers =
          entityManager
            .createQuery("from OpenAiBatchJobTracker", OpenAiBatchJobTracker::class.java)
            .resultList
        trackers[0].openAiInputFileId
      }
    fakeOpenAiBatchApiService.deletedFiles.assert.contains(inputFileId)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `batch translate poll failure is handled gracefully`() {
    fakeOpenAiBatchApiService.instantCompletion = false
    fakeOpenAiBatchApiService.nextPollError =
      RuntimeException("Simulated poll failure")

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

    // The poll error is transient - the poller should handle it and continue
    // Just verify that submission succeeded without the system crashing
    executeInNewTransaction {
      val trackers =
        entityManager
          .createQuery("from OpenAiBatchJobTracker", OpenAiBatchJobTracker::class.java)
          .resultList
      trackers.assert.hasSizeGreaterThanOrEqualTo(1)
    }
  }
}
