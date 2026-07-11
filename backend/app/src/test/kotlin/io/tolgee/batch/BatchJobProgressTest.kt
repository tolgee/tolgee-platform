package io.tolgee.batch

import io.tolgee.model.batch.BatchJob
import io.tolgee.model.batch.BatchJobChunkExecutionStatus
import io.tolgee.testing.ContextRecreatingTest
import io.tolgee.testing.assert
import io.tolgee.util.logger
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ContextConfiguration

/**
 * Tests for progress tracking accuracy and activity finalization in batch jobs.
 */
@SpringBootTest(
  properties = [
    "tolgee.cache.use-redis=true",
    "tolgee.cache.enabled=true",
    "tolgee.websocket.use-redis=true",
    "tolgee.batch.concurrency=10",
  ],
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
)
@ContextConfiguration(initializers = [AbstractBatchJobConcurrentTest.Companion.Initializer::class])
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ContextRecreatingTest
class BatchJobProgressTest : AbstractBatchJobConcurrentTest() {
  @Test
  fun `progress tracking accuracy under concurrent load`() {
    makePreTranslateProcessorPass()
    makeDeleteKeysProcessorPass()

    val jobsWithExpectedTotals = startJobsWithExpectedTotals()

    waitForAllJobsComplete(jobsWithExpectedTotals.keys.toList())
    assertAllJobsSuccessful(jobsWithExpectedTotals.keys.toList())
    assertAllProgressCountersMatch(jobsWithExpectedTotals)

    logger.info("Progress tracking accuracy verified for ${jobsWithExpectedTotals.size} concurrent jobs")
  }

  @Test
  fun `activity finalization with concurrent chunks`() {
    makePreTranslateProcessorPass()

    val job = runPreTranslateJob(testData.projectA, testData.getProjectAKeyIds().take(40), testData.projectACzech.id)

    waitForJobComplete(job)
    assertJobSuccess(job)

    val successfulExecutions = getSuccessfulExecutions(job)
    successfulExecutions.assert.isNotEmpty()

    logger.info("Activity finalization test completed with ${successfulExecutions.size} successful chunk executions")
  }

  private fun startJobsWithExpectedTotals(): Map<BatchJob, Int> {
    val result = mutableMapOf<BatchJob, Int>()

    val job1 = runPreTranslateJob(testData.projectA, testData.getProjectAKeyIds().take(50), testData.projectACzech.id)
    result[job1] = 50

    val job2 = runPreTranslateJob(testData.projectB, testData.getProjectBKeyIds().take(40), testData.projectBCzech.id)
    result[job2] = 40

    val job3 = runPreTranslateJob(testData.projectC, testData.getProjectCKeyIds().take(30), testData.projectCCzech.id)
    result[job3] = 30

    return result
  }

  private fun waitForAllJobsComplete(jobs: List<BatchJob>) {
    jobs.forEach { waitForJobComplete(it) }
  }

  private fun assertAllJobsSuccessful(jobs: List<BatchJob>) {
    jobs.forEach { assertJobSuccess(it) }
  }

  private fun assertAllProgressCountersMatch(jobsWithExpectedTotals: Map<BatchJob, Int>) {
    jobsWithExpectedTotals.forEach { (job, expectedTotal) ->
      val jobView = batchJobService.getView(job.id)
      jobView.batchJob.totalItems.assert
        .isEqualTo(expectedTotal)
      jobView.progress.assert.isEqualTo(expectedTotal)
      logger.info("Job ${job.id}: totalItems=${jobView.batchJob.totalItems}, progress=${jobView.progress}")
    }
  }

  private fun getSuccessfulExecutions(job: BatchJob) =
    batchJobService.getExecutions(job.id).filter { it.status == BatchJobChunkExecutionStatus.SUCCESS }
}
