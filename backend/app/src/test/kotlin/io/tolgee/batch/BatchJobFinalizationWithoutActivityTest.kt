package io.tolgee.batch

import io.tolgee.AbstractSpringTest
import io.tolgee.ActivityTestUtil
import io.tolgee.batch.data.BatchJobType
import io.tolgee.batch.events.OnBatchJobFinalized
import io.tolgee.batch.request.NoOpRequest
import io.tolgee.development.testDataBuilder.data.BatchJobsTestData
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.model.batch.BatchJob
import io.tolgee.testing.assert
import io.tolgee.util.executeInNewTransaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.event.EventListener
import java.util.concurrent.CopyOnWriteArrayList

@SpringBootTest(
  properties = [
    "tolgee.cache.enabled=true",
  ],
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
)
class BatchJobFinalizationWithoutActivityTest : AbstractSpringTest() {
  class FinalizedRecorder {
    val events = CopyOnWriteArrayList<OnBatchJobFinalized>()

    @EventListener
    fun onFinalized(event: OnBatchJobFinalized) {
      events.add(event)
    }
  }

  @TestConfiguration
  class RecorderConfiguration {
    @Bean
    fun finalizedRecorder() = FinalizedRecorder()
  }

  private lateinit var testData: BatchJobsTestData

  @Autowired
  lateinit var batchJobService: BatchJobService

  @Autowired
  lateinit var finalizedRecorder: FinalizedRecorder

  @Autowired
  lateinit var activityTestUtil: ActivityTestUtil

  @BeforeEach
  fun setup() {
    finalizedRecorder.events.clear()
    testData = BatchJobsTestData()
    testDataService.saveTestData(testData.root)
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `a job that logs no activity is still finalized`() {
    val job = runNoOpJob()

    waitForNotThrowing(pollTime = 100, timeout = 20_000) {
      executeInNewTransaction(platformTransactionManager) {
        batchJobService
          .getJobDtoNoCache(job.id)
          .status.completed.assert
          .isTrue()
      }
    }

    executeInNewTransaction(platformTransactionManager) {
      activityTestUtil.countRevisionsOfJob(job.id).assert.isEqualTo(0)
    }

    waitForNotThrowing(pollTime = 100, timeout = 20_000) {
      finalizedRecorder.events
        .first { it.job.id == job.id }
        .activityRevisionId
        .assert
        .isNull()
    }
  }

  private fun runNoOpJob(): BatchJob =
    executeInNewTransaction(platformTransactionManager) {
      batchJobService.startJob(
        request =
          NoOpRequest().apply {
            itemIds = (1L..3L).toList()
          },
        project = testData.projectBuilder.self,
        author = testData.user,
        type = BatchJobType.NO_OP,
      )
    }
}
