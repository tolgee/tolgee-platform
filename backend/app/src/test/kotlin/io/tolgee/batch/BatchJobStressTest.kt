package io.tolgee.batch

import io.tolgee.model.batch.BatchJob
import io.tolgee.model.batch.BatchJobStatus
import io.tolgee.testing.ContextRecreatingTest
import io.tolgee.testing.assert
import io.tolgee.util.logger
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ContextConfiguration

/**
 * Stress tests for batch job execution under high load conditions.
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
class BatchJobStressTest : AbstractBatchJobConcurrentTest() {
  @Test
  fun `stress test - many jobs many projects`() {
    setupAllProcessors()

    val allJobs = startJobsAcrossAllProjects()
    val startTime = System.currentTimeMillis()

    waitForAllJobsComplete(allJobs, timeoutMs = 30_000)
    assertAllJobsSuccessful(allJobs)

    logStressTestResults(allJobs, startTime)
    assertNoDeadlocksOrStuckJobs(allJobs)
  }

  @Test
  fun `extended stress test with multiple job waves`() {
    val chunksPerJob = 200
    val jobsPerWave = 3
    val totalWaves = 3

    logExtendedStressTestHeader(totalWaves, jobsPerWave)

    val allJobs = runMultipleWaves(totalWaves, jobsPerWave, chunksPerJob)
    val startTime = System.currentTimeMillis()

    assertAllJobsSuccessful(allJobs)

    logExtendedStressTestResults(allJobs, startTime, totalWaves)
  }

  private fun setupAllProcessors() {
    makePreTranslateProcessorPass()
    makeDeleteKeysProcessorPass()
    makeMtProcessorPass()
  }

  private fun startJobsAcrossAllProjects(): List<BatchJob> =
    listOf(
      runPreTranslateJob(testData.projectA, testData.getProjectAKeyIds().take(20), testData.projectACzech.id),
      runPreTranslateJob(testData.projectB, testData.getProjectBKeyIds().take(20), testData.projectBCzech.id),
      runPreTranslateJob(testData.projectC, testData.getProjectCKeyIds().take(20), testData.projectCCzech.id),
    )

  private fun waitForAllJobsComplete(
    jobs: List<BatchJob>,
    timeoutMs: Long,
  ) {
    jobs.forEach { job ->
      waitForJobComplete(job, timeoutMs = timeoutMs)
    }
  }

  private fun assertAllJobsSuccessful(jobs: List<BatchJob>) {
    jobs.forEach { assertJobSuccess(it) }
  }

  private fun logStressTestResults(
    jobs: List<BatchJob>,
    startTime: Long,
  ) {
    val totalTime = System.currentTimeMillis() - startTime
    val totalChunks = jobs.sumOf { batchJobService.getJobDto(it.id).totalChunks }
    val chunksPerSecond = totalChunks * 1000.0 / totalTime

    logger.info("=".repeat(60))
    logger.info("STRESS TEST RESULTS:")
    logger.info("  Total jobs: ${jobs.size}")
    logger.info("  Total chunks: $totalChunks")
    logger.info("  Total time: ${totalTime}ms")
    logger.info("  Throughput: ${"%.2f".format(chunksPerSecond)} chunks/second")
    logger.info("=".repeat(60))
  }

  private fun assertNoDeadlocksOrStuckJobs(jobs: List<BatchJob>) {
    jobs.forEach { job ->
      val jobDto = batchJobService.getJobDto(job.id)
      jobDto.status.completed.assert
        .isTrue()
    }
  }

  private fun logExtendedStressTestHeader(
    totalWaves: Int,
    jobsPerWave: Int,
  ) {
    logger.info("=".repeat(70))
    logger.info("EXTENDED STRESS TEST - $totalWaves waves of $jobsPerWave jobs each")
    logger.info("=".repeat(70))
  }

  private fun runMultipleWaves(
    totalWaves: Int,
    jobsPerWave: Int,
    chunksPerJob: Int,
  ): List<BatchJob> {
    val allJobs = mutableListOf<BatchJob>()
    val startTime = System.currentTimeMillis()

    repeat(totalWaves) { wave ->
      val waveJobs = runSingleWave(wave, totalWaves, chunksPerJob)
      allJobs.addAll(waveJobs)
      waitBetweenWavesIfNeeded(wave, totalWaves)
    }

    logExtendedStressTestResults(allJobs, startTime, totalWaves)
    return allJobs
  }

  private fun runSingleWave(
    waveIndex: Int,
    totalWaves: Int,
    chunksPerJob: Int,
  ): List<BatchJob> {
    logger.info("Wave ${waveIndex + 1}/$totalWaves: Starting jobs...")

    val waveJobs =
      listOf(
        runNoOpJob(testData.projectA, chunksPerJob),
        runNoOpJob(testData.projectB, chunksPerJob),
        runNoOpJob(testData.projectC, chunksPerJob),
      )

    waveJobs.forEach { job ->
      waitForJobComplete(job, timeoutMs = 15_000)
      assertJobSuccess(job)
    }

    logger.info("Wave ${waveIndex + 1}/$totalWaves: Completed")
    return waveJobs
  }

  private fun waitBetweenWavesIfNeeded(
    currentWave: Int,
    totalWaves: Int,
  ) {
    if (currentWave < totalWaves - 1) {
      Thread.sleep(500)
    }
  }

  private fun logExtendedStressTestResults(
    jobs: List<BatchJob>,
    startTime: Long,
    totalWaves: Int,
  ) {
    val totalTime = System.currentTimeMillis() - startTime
    val totalChunks = jobs.sumOf { batchJobService.getJobDto(it.id).totalChunks }
    val chunksPerSecond = totalChunks * 1000.0 / totalTime

    jobs.forEach { job ->
      batchJobService
        .getJobDto(job.id)
        .status.assert
        .isEqualTo(BatchJobStatus.SUCCESS)
    }

    logger.info("=".repeat(70))
    logger.info("EXTENDED STRESS TEST RESULTS:")
    logger.info("  Total waves: $totalWaves")
    logger.info("  Total jobs: ${jobs.size}")
    logger.info("  Total chunks: $totalChunks")
    logger.info("  Total time: ${totalTime}ms")
    logger.info("  Throughput: ${"%.2f".format(chunksPerSecond)} chunks/second")
    logger.info("=".repeat(70))
  }
}
