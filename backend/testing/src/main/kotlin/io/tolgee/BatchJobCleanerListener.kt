package io.tolgee

import io.tolgee.BatchJobTestUtil.pauseAndClearBatchJobs
import io.tolgee.BatchJobTestUtil.resumeBatchJobs
import org.springframework.test.context.TestContext
import org.springframework.test.context.TestExecutionListener

class BatchJobCleanerListener : TestExecutionListener {
  override fun beforeTestMethod(testContext: TestContext) {
    pauseAndClearBatchJobs(testContext)
    resumeBatchJobs(testContext)
  }
}
