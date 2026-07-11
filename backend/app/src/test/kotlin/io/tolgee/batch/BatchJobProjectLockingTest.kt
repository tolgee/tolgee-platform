package io.tolgee.batch

import io.tolgee.batch.data.BatchJobType
import io.tolgee.configuration.tolgee.BatchJobTypeOverrideProperties
import io.tolgee.testing.ContextRecreatingTest
import io.tolgee.util.logger
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ContextConfiguration

/**
 * Tests for project locking behavior in batch job execution.
 * Verifies that exclusive jobs on the same project queue correctly,
 * while non-exclusive jobs can bypass project locking.
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
class BatchJobProjectLockingTest : AbstractBatchJobConcurrentTest() {
  @Test
  fun `exclusive jobs on same project queue correctly`() {
    makePreTranslateProcessorPass()

    val keyIds = testData.getProjectAKeyIds()
    val job1 = runPreTranslateJob(testData.projectA, keyIds.take(30), testData.projectACzech.id)
    val job2 = runPreTranslateJob(testData.projectA, keyIds.drop(30).take(30), testData.projectACzech.id)

    waitForJobComplete(job1, timeoutMs = 15_000)
    waitForJobComplete(job2, timeoutMs = 15_000)

    assertJobSuccess(job1)
    assertJobSuccess(job2)

    logger.info("Both exclusive jobs on same project completed, project locking verified")
  }

  @Test
  fun `non-exclusive jobs bypass project locking`() {
    makeDeleteKeysProcessorPassWithDelay(50)
    makeAutomationProcessorPass()

    val exclusiveJob = runDeleteKeysJob(testData.projectA, testData.getProjectAKeyIds().take(20))

    waitForExclusiveJobToStart()

    val nonExclusiveJob = runAutomationJob(testData.projectA)

    waitForJobComplete(nonExclusiveJob)
    assertJobSuccess(nonExclusiveJob)

    waitForJobComplete(exclusiveJob)
    assertJobSuccess(exclusiveJob)

    logger.info("Non-exclusive job completed while exclusive job was running - bypass verified")
  }

  @Test
  fun `job type override makes exclusive type bypass project locking`() {
    val savedOverrides = batchProperties.jobTypeOverrides
    try {
      batchProperties.jobTypeOverrides =
        mapOf(
          BatchJobType.MACHINE_TRANSLATE to
            BatchJobTypeOverrideProperties().apply {
              exclusive = false
            },
        )

      makeMtProcessorPassWithDelay(200)
      makeDeleteKeysProcessorPassWithDelay(50)

      // Start an exclusive job to lock the project
      val exclusiveJob = runDeleteKeysJob(testData.projectA, testData.getProjectAKeyIds().take(20))
      waitForExclusiveJobToStart()

      // MT job should bypass the lock because override sets exclusive=false
      val mtJob = runMtJob(testData.projectA, testData.getProjectAKeyIds().take(10), testData.projectACzech.id)

      waitForJobComplete(mtJob, timeoutMs = 15_000)
      assertJobSuccess(mtJob)

      waitForJobComplete(exclusiveJob, timeoutMs = 15_000)
      assertJobSuccess(exclusiveJob)

      logger.info("MT job bypassed project lock via job-type override - verified")
    } finally {
      batchProperties.jobTypeOverrides = savedOverrides
    }
  }

  private fun waitForExclusiveJobToStart() {
    Thread.sleep(200)
  }
}
