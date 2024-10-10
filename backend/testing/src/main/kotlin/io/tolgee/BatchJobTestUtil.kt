package io.tolgee

import io.tolgee.batch.BatchJobChunkExecutionQueue
import io.tolgee.batch.BatchJobConcurrentLauncher
import org.springframework.test.context.TestContext

object BatchJobTestUtil {
  fun resumeBatchJobs(testContext: TestContext) {
    getBatchJobConcurrentLauncher(testContext).pause = false
  }

  fun pauseAndClearBatchJobs(testContext: TestContext) {
    getBatchJobConcurrentLauncher(testContext).pause = true
    getBatchJobExecutionQueue(testContext).clear()
  }

  private fun getBatchJobExecutionQueue(testContext: TestContext): BatchJobChunkExecutionQueue {
    return testContext.applicationContext.getBean(BatchJobChunkExecutionQueue::class.java)
  }

  private fun getBatchJobConcurrentLauncher(testContext: TestContext): BatchJobConcurrentLauncher {
    return testContext.applicationContext.getBean(BatchJobConcurrentLauncher::class.java)
  }
}
