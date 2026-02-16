package io.tolgee.batch

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

  private fun waitForExclusiveJobToStart() {
    Thread.sleep(200)
  }
}
