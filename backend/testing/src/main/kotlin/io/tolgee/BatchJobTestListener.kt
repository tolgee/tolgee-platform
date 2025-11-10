package io.tolgee

import io.tolgee.batch.ApplicationBatchJobRunner
import io.tolgee.batch.BatchJobChunkExecutionQueue
import io.tolgee.batch.BatchJobConcurrentLauncher
import io.tolgee.util.Logging
import io.tolgee.util.logger
import org.springframework.test.context.TestContext
import org.springframework.test.context.TestExecutionListener

/**
 * This listener us used to run application batch job runner always once and with the current application context
 *
 * Spring caches application context in tests and may create a new instance of ApplicationBatchJobRunner
 *
 * To not run the batch jobs with different test context, this class helps us to clean the context and
 * run the batch job runner correctly
 */
class BatchJobTestListener :
  Logging,
  TestExecutionListener {
  override fun beforeTestMethod(testContext: TestContext) {
    logger.info("Pausing and clearing batch jobs")
    pauseAndClearBatchJobs(testContext)

    logStopping()
    ApplicationBatchJobRunner.stopAll()

    logger.info("Running application batch job runner for current context")
    testContext.applicationBatchJobRunner.run()
  }

  override fun afterTestMethod(testContext: TestContext) {
    logStopping()
    // to be super safe, rather stop them all if by any chance there is more than one instance running
    ApplicationBatchJobRunner.stopAll()
  }

  private fun logStopping() {
    logger.info("Stopping all application batch job runners")
  }

  private inline fun <reified T> TestContext.getBean(): T {
    return this.applicationContext.getBean(T::class.java)
  }

  private val TestContext.applicationBatchJobRunner: ApplicationBatchJobRunner
    get() = this.getBean()

  private fun pauseAndClearBatchJobs(testContext: TestContext) {
    testContext.batchJobConcurrentLauncher.pause = true
    testContext.batchJobChunkExecutionQueue.clear()
  }

  private val TestContext.batchJobChunkExecutionQueue
    get() = this.getBean<BatchJobChunkExecutionQueue>()

  private val TestContext.batchJobConcurrentLauncher
    get() = this.getBean<BatchJobConcurrentLauncher>()
}
