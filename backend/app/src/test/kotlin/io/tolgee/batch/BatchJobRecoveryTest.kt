package io.tolgee.batch

import io.tolgee.model.batch.BatchJob
import io.tolgee.model.batch.BatchJobChunkExecutionStatus
import io.tolgee.model.batch.BatchJobStatus
import io.tolgee.testing.ContextRecreatingTest
import io.tolgee.testing.assert
import io.tolgee.util.logger
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ContextConfiguration

/**
 * Tests for job recovery after processing interruptions and node failures.
 * Includes production-like load tests with simulated failures.
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
class BatchJobRecoveryTest : AbstractBatchJobConcurrentTest() {
  @Test
  fun `job recovery after processing restart`() {
    val chunkCount = 300
    logger.info("Starting job recovery test with $chunkCount chunks...")

    val job = runNoOpJob(testData.projectA, chunkCount)
    logger.info("Started job ${job.id}")

    letJobRunPartially()

    pauseAndWaitForRunningJobsToStop()
    val progressAtPause = logProgressAtPause(job, chunkCount)

    resumeProcessing()

    waitForJobComplete(job, timeoutMs = 15_000)
    assertJobRecovered(job, chunkCount)
  }

  @Test
  fun `production load test with node failure and redis recovery`() {
    val chunksPerProject = 500
    val totalChunks = chunksPerProject * 3

    logProductionLoadTestHeader(totalChunks)

    val startTime = System.currentTimeMillis()
    val allJobs = startLargeJobsOnAllProjects(chunksPerProject)

    letJobsRunPartially()
    val progressBeforeFailure = logProgressBeforeFailure(allJobs)

    simulateNodeFailure()
    simulateRedisRestart(allJobs)
    repopulateQueueFromDatabase()
    resumeProcessing()

    waitForAllJobsComplete(allJobs)

    verifyAllJobsCompletedSuccessfully(allJobs, chunksPerProject)
    verifyExecutionCounts(allJobs)
    logProductionLoadTestResults(allJobs, totalChunks, startTime)
  }

  private fun letJobRunPartially() {
    Thread.sleep(1000)
  }

  private fun letJobsRunPartially() {
    Thread.sleep(2000)
  }

  private fun pauseAndWaitForRunningJobsToStop() {
    batchJobConcurrentLauncher.pause = true
    waitForRunningJobsEmpty()
  }

  private fun logProgressAtPause(
    job: BatchJob,
    chunkCount: Int,
  ): Int {
    val progressAtPause = batchJobService.getView(job.id).progress
    logger.info("Paused at progress: $progressAtPause/$chunkCount")
    return progressAtPause
  }

  private fun resumeProcessing() {
    batchJobConcurrentLauncher.pause = false
  }

  private fun assertJobRecovered(
    job: BatchJob,
    chunkCount: Int,
  ) {
    val finalView = batchJobService.getView(job.id)
    finalView.batchJob.status.assert
      .isEqualTo(BatchJobStatus.SUCCESS)
    finalView.progress.assert.isEqualTo(chunkCount)
    logger.info("Job recovered and completed: progress=$chunkCount, status=SUCCESS")
  }

  private fun logProductionLoadTestHeader(totalChunks: Int) {
    logger.info("=".repeat(70))
    logger.info("PRODUCTION LOAD TEST - Starting with $totalChunks total chunks")
    logger.info("=".repeat(70))
    logger.info("Phase 1: Starting large jobs...")
  }

  private fun startLargeJobsOnAllProjects(chunksPerProject: Int): List<BatchJob> {
    val jobs =
      listOf(
        runNoOpJob(testData.projectA, chunksPerProject),
        runNoOpJob(testData.projectB, chunksPerProject),
        runNoOpJob(testData.projectC, chunksPerProject),
      )
    logger.info("Started ${jobs.size} jobs with $chunksPerProject chunks each")
    return jobs
  }

  private fun logProgressBeforeFailure(jobs: List<BatchJob>): Map<Long, Int> {
    logger.info("Phase 2: Running jobs, waiting for partial completion...")
    val progress =
      jobs.associate { job ->
        val view = batchJobService.getView(job.id)
        job.id to view.progress
      }
    logger.info("Progress before simulated failure: $progress")
    return progress
  }

  private fun simulateNodeFailure() {
    logger.info("Phase 3: Simulating node failure (pausing batch job launcher)...")
    batchJobConcurrentLauncher.pause = true
    waitForRunningJobsEmpty()
    logger.info("All running coroutines stopped")
  }

  private fun simulateRedisRestart(jobs: List<BatchJob>) {
    logger.info("Phase 4: Clearing Redis batch job state (simulating Redis restart)...")
    clearRedisBatchJobState(jobs.map { it.id })
    batchJobStateProvider.clearAllState()
    logger.info("Redis state cleared")
  }

  private fun repopulateQueueFromDatabase() {
    logger.info("Phase 5: Repopulating queue from database...")
    batchJobChunkExecutionQueue.clear()
    batchJobChunkExecutionQueue.populateQueue()
    logger.info("Queue repopulated with ${batchJobChunkExecutionQueue.size} items from database")
  }

  private fun waitForAllJobsComplete(jobs: List<BatchJob>) {
    logger.info("Phase 7: Waiting for all jobs to complete...")
    jobs.forEach { waitForJobComplete(it, timeoutMs = 30_000) }
  }

  private fun verifyAllJobsCompletedSuccessfully(
    jobs: List<BatchJob>,
    chunksPerProject: Int,
  ) {
    logger.info("Phase 8: Verifying job completion and data integrity...")
    jobs.forEach { job ->
      val jobDto = batchJobService.getJobDto(job.id)
      val jobView = batchJobService.getView(job.id)

      jobDto.status.assert.isEqualTo(BatchJobStatus.SUCCESS)
      jobView.progress.assert.isEqualTo(chunksPerProject)

      logger.info(
        "Job ${job.id}: status=${jobDto.status}, " +
          "progress=${jobView.progress}/${jobDto.totalItems}, " +
          "chunks=${jobDto.totalChunks}",
      )
    }
  }

  private fun verifyExecutionCounts(jobs: List<BatchJob>) {
    jobs.forEach { job ->
      val executions = batchJobService.getExecutions(job.id)
      val successCount = executions.count { it.status == BatchJobChunkExecutionStatus.SUCCESS }
      val jobDto = batchJobService.getJobDto(job.id)

      logger.info("Job ${job.id}: $successCount successful executions out of ${jobDto.totalChunks} chunks")
      successCount.assert.isGreaterThanOrEqualTo(jobDto.totalChunks)
    }
  }

  private fun logProductionLoadTestResults(
    jobs: List<BatchJob>,
    totalChunks: Int,
    startTime: Long,
  ) {
    val totalTime = System.currentTimeMillis() - startTime
    val chunksPerSecond = totalChunks * 1000.0 / totalTime

    logger.info("=".repeat(70))
    logger.info("PRODUCTION LOAD TEST RESULTS:")
    logger.info("  Total jobs: ${jobs.size}")
    logger.info("  Total chunks: $totalChunks")
    logger.info("  Total time (including simulated failure): ${totalTime}ms")
    logger.info("  Effective throughput: ${"%.2f".format(chunksPerSecond)} chunks/second")
    logger.info("  Node failure simulated: YES")
    logger.info("  Redis state cleared: YES")
    logger.info("  Recovery from DB: SUCCESS")
    logger.info("=".repeat(70))
  }
}
