package io.tolgee.batch

import io.tolgee.testing.ContextRecreatingTest
import io.tolgee.testing.assert
import io.tolgee.util.logger
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ContextConfiguration
import java.util.concurrent.atomic.AtomicInteger

/**
 * Tests for concurrency limits and job character fairness in batch job execution.
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
class BatchJobConcurrencyTest : AbstractBatchJobConcurrentTest() {
  @Test
  fun `maxPerJobConcurrency limits chunk parallelism`() {
    val originalMaxPerMtConcurrency = batchProperties.maxPerMtJobConcurrency
    val maxConcurrency = 2
    batchProperties.maxPerMtJobConcurrency = maxConcurrency

    try {
      val trackers = ConcurrencyTrackers()
      makeMtProcessorPassWithConcurrencyTracking(trackers.current, trackers.max)

      val job = runMtJob(testData.projectA, testData.getProjectAKeyIds().take(50), testData.projectACzech.id)

      waitForJobComplete(job)
      assertJobSuccess(job)

      assertMaxConcurrencyNotExceeded(trackers.max.get(), maxConcurrency)
    } finally {
      batchProperties.maxPerMtJobConcurrency = originalMaxPerMtConcurrency
    }
  }

  @Test
  fun `job character fairness under load`() {
    val counters = CharacterExecutionCounters()
    makeMtProcessorPassWithCounter(counters.slow)
    makePreTranslateProcessorPassWithCounter(counters.fast)

    val jobs = startJobsWithDifferentCharacters()

    waitForAllJobsComplete(jobs)
    assertAllJobsSuccessful(jobs)

    logCharacterFairnessResults(counters)
  }

  private fun startJobsWithDifferentCharacters(): List<io.tolgee.model.batch.BatchJob> {
    val slowJob = runMtJob(testData.projectA, testData.getProjectAKeyIds().take(30), testData.projectACzech.id)
    val fastJob1 =
      runPreTranslateJob(testData.projectB, testData.getProjectBKeyIds().take(30), testData.projectBCzech.id)
    val fastJob2 =
      runPreTranslateJob(testData.projectC, testData.getProjectCKeyIds().take(30), testData.projectCCzech.id)
    return listOf(slowJob, fastJob1, fastJob2)
  }

  private fun waitForAllJobsComplete(jobs: List<io.tolgee.model.batch.BatchJob>) {
    jobs.forEach { waitForJobComplete(it) }
  }

  private fun assertAllJobsSuccessful(jobs: List<io.tolgee.model.batch.BatchJob>) {
    jobs.forEach { assertJobSuccess(it) }
  }

  private fun assertMaxConcurrencyNotExceeded(
    observed: Int,
    limit: Int,
  ) {
    observed.assert.isLessThanOrEqualTo(limit)
    logger.info("maxPerJobConcurrency enforced: max observed = $observed, limit = $limit")
  }

  private fun logCharacterFairnessResults(counters: CharacterExecutionCounters) {
    logger.info(
      "Job character fairness test completed - SLOW executions: ${counters.slow.get()}, FAST executions: ${counters.fast.get()}",
    )
  }

  private data class ConcurrencyTrackers(
    val current: AtomicInteger = AtomicInteger(0),
    val max: AtomicInteger = AtomicInteger(0),
  )

  private data class CharacterExecutionCounters(
    val slow: AtomicInteger = AtomicInteger(0),
    val fast: AtomicInteger = AtomicInteger(0),
  )
}
