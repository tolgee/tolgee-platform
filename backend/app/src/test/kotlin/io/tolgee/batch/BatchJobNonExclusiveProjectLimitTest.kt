package io.tolgee.batch

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
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Tests that non-exclusive jobs are limited per project so that
 * a single project with many non-exclusive jobs doesn't starve other projects.
 */
@SpringBootTest(
  properties = [
    "tolgee.cache.use-redis=true",
    "tolgee.cache.enabled=true",
    "tolgee.websocket.use-redis=true",
    "tolgee.batch.concurrency=10",
    "tolgee.batch.max-non-exclusive-jobs-per-project=2",
  ],
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
)
@ContextConfiguration(initializers = [AbstractBatchJobConcurrentTest.Companion.Initializer::class])
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ContextRecreatingTest
class BatchJobNonExclusiveProjectLimitTest : AbstractBatchJobConcurrentTest() {
  @Test
  fun `limits concurrent non-exclusive jobs per project`() {
    val concurrentJobIds = ConcurrentHashMap.newKeySet<Long>()
    val maxConcurrentDistinctJobs = AtomicInteger(0)
    val currentConcurrentDistinctJobs = ConcurrentHashMap<Long, AtomicInteger>()

    doAnswer { invocation ->
      val job = invocation.getArgument<io.tolgee.batch.data.BatchJobDto>(0)
      val counter = currentConcurrentDistinctJobs.computeIfAbsent(job.id) { AtomicInteger(0) }
      // Only count the first chunk of each job entering
      if (counter.incrementAndGet() == 1) {
        concurrentJobIds.add(job.id)
        maxConcurrentDistinctJobs.updateAndGet { max -> maxOf(max, concurrentJobIds.size) }
      }
      try {
        Thread.sleep(200)
      } finally {
        if (counter.decrementAndGet() == 0) {
          concurrentJobIds.remove(job.id)
        }
      }
    }.whenever(noOpChunkProcessor).process(any(), any(), any())

    // Start 5 non-exclusive NO_OP jobs for projectA
    val jobs = (1..5).map { runNoOpJob(testData.projectA, 3) }

    jobs.forEach { waitForJobComplete(it, timeoutMs = 30_000) }

    jobs.forEach { assertJobSuccess(it) }

    logger.info("Max concurrent distinct non-exclusive jobs for project: ${maxConcurrentDistinctJobs.get()}")
    maxConcurrentDistinctJobs
      .get()
      .assert
      .isLessThanOrEqualTo(batchProperties.maxNonExclusiveJobsPerProject)
  }

  @Test
  fun `non-exclusive jobs from different projects can run concurrently`() {
    val currentConcurrency = AtomicInteger(0)
    val maxConcurrency = AtomicInteger(0)

    doAnswer {
      val current = currentConcurrency.incrementAndGet()
      maxConcurrency.updateAndGet { max -> maxOf(max, current) }
      try {
        Thread.sleep(100)
      } finally {
        currentConcurrency.decrementAndGet()
      }
    }.whenever(noOpChunkProcessor).process(any(), any(), any())

    // Start non-exclusive jobs on different projects
    val jobA = runNoOpJob(testData.projectA, 2)
    val jobB = runNoOpJob(testData.projectB, 2)
    val jobC = runNoOpJob(testData.projectC, 2)

    waitForJobComplete(jobA, timeoutMs = 15_000)
    waitForJobComplete(jobB, timeoutMs = 15_000)
    waitForJobComplete(jobC, timeoutMs = 15_000)

    assertJobSuccess(jobA)
    assertJobSuccess(jobB)
    assertJobSuccess(jobC)

    // All 3 projects should be able to run concurrently (limit is per-project)
    logger.info("Max concurrent non-exclusive chunks across projects: ${maxConcurrency.get()}")
    maxConcurrency.get().assert.isGreaterThan(1)
  }
}
