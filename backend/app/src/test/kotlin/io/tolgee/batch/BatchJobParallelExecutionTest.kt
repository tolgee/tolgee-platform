package io.tolgee.batch

import io.tolgee.testing.ContextRecreatingTest
import io.tolgee.util.logger
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ContextConfiguration

/**
 * Tests for parallel execution of batch jobs across different projects.
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
class BatchJobParallelExecutionTest : AbstractBatchJobConcurrentTest() {
  @Test
  fun `multiple job types from different projects run in parallel`() {
    setupProcessorsWithDelay()

    val jobs = startJobsOnDifferentProjects()

    waitForAllJobsComplete(jobs)
    assertAllJobsSuccessful(jobs)
    assertAllProgressCountersAccurate(jobs)

    logger.info("All 3 jobs from different projects completed successfully in parallel")
  }

  private fun setupProcessorsWithDelay() {
    makePreTranslateProcessorPassWithDelay(10)
    makeDeleteKeysProcessorPassWithDelay(10)
    makeMtProcessorPassWithDelay(10)
  }

  private fun startJobsOnDifferentProjects() =
    listOf(
      runPreTranslateJob(testData.projectA, testData.getProjectAKeyIds().take(50), testData.projectACzech.id),
      runPreTranslateJob(testData.projectB, testData.getProjectBKeyIds().take(40), testData.projectBCzech.id),
      runDeleteKeysJob(testData.projectC, testData.getProjectCKeyIds().take(30)),
    )

  private fun waitForAllJobsComplete(jobs: List<io.tolgee.model.batch.BatchJob>) {
    jobs.forEach { waitForJobComplete(it) }
  }

  private fun assertAllJobsSuccessful(jobs: List<io.tolgee.model.batch.BatchJob>) {
    jobs.forEach { assertJobSuccess(it) }
  }

  private fun assertAllProgressCountersAccurate(jobs: List<io.tolgee.model.batch.BatchJob>) {
    jobs.forEach { assertProgressCounterAccurate(it) }
  }
}
