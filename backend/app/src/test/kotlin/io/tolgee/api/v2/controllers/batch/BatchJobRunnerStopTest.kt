package io.tolgee.api.v2.controllers.batch

import io.tolgee.AbstractSpringTest
import io.tolgee.batch.ApplicationBatchJobRunner
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class BatchJobRunnerStopTest : AbstractSpringTest() {
  @Test
  fun `it stops`() {
    ApplicationBatchJobRunner.stopAll()
    ApplicationBatchJobRunner.runningInstances.assert.hasSize(0)
  }
}
