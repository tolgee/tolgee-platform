package io.tolgee.batch

import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.testing.ContextRecreatingTest
import io.tolgee.testing.assert
import io.tolgee.util.logger
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ContextConfiguration

/**
 * Tests for round-robin scheduling behavior in batch job execution.
 * Verifies that pollRoundRobin() interleaves chunks from different jobs
 * instead of draining one job before starting another.
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
class BatchJobRoundRobinTest : AbstractBatchJobConcurrentTest() {
  companion object {
    private const val LARGE_JOB_CHUNKS = 20
    private const val SMALL_JOB_CHUNKS = 5
    private const val POLL_COUNT = 10
    private const val MIN_EXPECTED_SWITCHES = 4
  }

  @Test
  fun `round-robin scheduling prioritizes second job over more chunks from first job`() {
    pauseLauncher()

    val jobs = startJobsWithDifferentSizes()
    waitForQueueToPopulate(expectedSize = LARGE_JOB_CHUNKS + SMALL_JOB_CHUNKS)

    val polledJobIds = pollItemsRoundRobin(POLL_COUNT)
    val switches = countJobSwitches(polledJobIds)

    assertRoundRobinBehavior(switches, polledJobIds, jobs)
    logRoundRobinResults(switches)
  }

  private fun pauseLauncher() {
    batchJobConcurrentLauncher.pause = true
  }

  private fun startJobsWithDifferentSizes(): Jobs {
    val largeJob = runNoOpJob(testData.projectA, LARGE_JOB_CHUNKS)
    val smallJob = runNoOpJob(testData.projectB, SMALL_JOB_CHUNKS)
    return Jobs(largeJob, smallJob)
  }

  private fun waitForQueueToPopulate(expectedSize: Int) {
    waitForNotThrowing(pollTime = 50, timeout = 5_000) {
      batchJobChunkExecutionQueue.size.assert.isGreaterThanOrEqualTo(expectedSize)
    }
    logger.info("Queue populated with ${batchJobChunkExecutionQueue.size} items")
  }

  private fun pollItemsRoundRobin(count: Int): List<Long> {
    val polledJobIds = mutableListOf<Long>()
    repeat(count) {
      batchJobChunkExecutionQueue.pollRoundRobin()?.let { item ->
        polledJobIds.add(item.jobId)
      }
    }
    logger.info("Polled job IDs in order: $polledJobIds")
    return polledJobIds
  }

  private fun countJobSwitches(jobIds: List<Long>): Int {
    var switches = 0
    for (i in 1 until jobIds.size) {
      if (jobIds[i] != jobIds[i - 1]) {
        switches++
      }
    }
    return switches
  }

  private fun assertRoundRobinBehavior(
    switches: Int,
    polledJobIds: List<Long>,
    jobs: Jobs,
  ) {
    switches.assert.isGreaterThanOrEqualTo(MIN_EXPECTED_SWITCHES)
    polledJobIds.contains(jobs.large.id).assert.isTrue()
    polledJobIds.contains(jobs.small.id).assert.isTrue()
  }

  private fun logRoundRobinResults(switches: Int) {
    logger.info("Job switches in first $POLL_COUNT polls: $switches")
    logger.info("Round-robin scheduling verified: $switches job switches in first $POLL_COUNT polls")
  }

  private data class Jobs(
    val large: io.tolgee.model.batch.BatchJob,
    val small: io.tolgee.model.batch.BatchJob,
  )
}
