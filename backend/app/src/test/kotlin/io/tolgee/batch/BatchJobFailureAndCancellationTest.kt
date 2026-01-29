package io.tolgee.batch

import io.tolgee.model.batch.BatchJob
import io.tolgee.testing.ContextRecreatingTest
import io.tolgee.testing.assert
import io.tolgee.util.logger
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.whenever
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ContextConfiguration
import java.util.concurrent.atomic.AtomicInteger

/**
 * Tests for failure handling, retry behavior, and cancellation in batch jobs.
 */
@SpringBootTest(
  properties = [
    "tolgee.cache.use-redis=true",
    "tolgee.cache.enabled=true",
    "tolgee.websocket.use-redis=true",
    "spring.redis.port=56379",
    "tolgee.batch.concurrency=10",
  ],
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
)
@ContextConfiguration(initializers = [AbstractBatchJobConcurrentTest.Companion.Initializer::class])
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ContextRecreatingTest
class BatchJobFailureAndCancellationTest : AbstractBatchJobConcurrentTest() {
  @Test
  fun `failure and retry with concurrent jobs`() {
    makePreTranslateProcessorPass()

    val failCount = AtomicInteger(0)
    makeDeleteKeysProcessorFailThenSucceed(failCount, failuresBeforeSuccess = 2)

    val successfulJobs = startSuccessfulJobs()
    val failingJob = runDeleteKeysJob(testData.projectC, testData.getProjectCKeyIds().take(5))

    waitForSuccessfulJobs(successfulJobs)
    assertSuccessfulJobsCompleted(successfulJobs)

    waitForJobComplete(failingJob, timeoutMs = 15_000)
    assertJobSuccess(failingJob)

    logger.info("Failure and retry test completed - failing job recovered after ${failCount.get()} failures")
  }

  @Test
  fun `cancellation during concurrent execution`() {
    makePreTranslateProcessorPass()
    makeDeleteKeysProcessorPassWithDelay(200)

    val continuingJobs = startContinuingJobs()
    val jobToCancel = runDeleteKeysJob(testData.projectC, testData.getProjectCKeyIds().take(20))

    waitForJobsToStart()

    batchJobCancellationManager.cancel(jobToCancel.id)

    waitForJobCancelledOrCompleted(jobToCancel)

    waitForContinuingJobsComplete(continuingJobs)
    assertContinuingJobsSuccessful(continuingJobs)

    assertJobCancelledOrCompleted(jobToCancel)
  }

  private fun makeDeleteKeysProcessorFailThenSucceed(
    failCount: AtomicInteger,
    failuresBeforeSuccess: Int,
  ) {
    doAnswer {
      if (failCount.incrementAndGet() <= failuresBeforeSuccess) {
        throw RuntimeException("Simulated failure #${failCount.get()}")
      }
    }.whenever(deleteKeysChunkProcessor).process(any(), any(), any())
  }

  private fun startSuccessfulJobs(): List<BatchJob> =
    listOf(
      runPreTranslateJob(testData.projectA, testData.getProjectAKeyIds().take(20), testData.projectACzech.id),
      runPreTranslateJob(testData.projectB, testData.getProjectBKeyIds().take(20), testData.projectBCzech.id),
    )

  private fun waitForSuccessfulJobs(jobs: List<BatchJob>) {
    jobs.forEach { waitForJobComplete(it) }
  }

  private fun assertSuccessfulJobsCompleted(jobs: List<BatchJob>) {
    jobs.forEach { assertJobSuccess(it) }
  }

  private fun startContinuingJobs(): List<BatchJob> =
    listOf(
      runPreTranslateJob(testData.projectA, testData.getProjectAKeyIds().take(30), testData.projectACzech.id),
      runPreTranslateJob(testData.projectB, testData.getProjectBKeyIds().take(30), testData.projectBCzech.id),
    )

  private fun waitForJobsToStart() {
    Thread.sleep(300)
  }

  private fun waitForContinuingJobsComplete(jobs: List<BatchJob>) {
    jobs.forEach { waitForJobComplete(it) }
  }

  private fun assertContinuingJobsSuccessful(jobs: List<BatchJob>) {
    jobs.forEach { assertJobSuccess(it) }
  }

  private fun assertJobCancelledOrCompleted(job: BatchJob) {
    val jobDto = batchJobService.getJobDto(job.id)
    jobDto.status.completed.assert
      .isTrue()
    logger.info("Cancellation test completed - job status: ${jobDto.status}")
  }
}
