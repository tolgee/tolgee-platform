package io.tolgee.batch

import io.tolgee.AbstractSpringTest
import io.tolgee.batch.events.OnBatchJobFinalized
import io.tolgee.development.testDataBuilder.data.BatchJobsTestData
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.model.batch.BatchJobStatus
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
import java.util.Date
import java.util.concurrent.CopyOnWriteArrayList

@SpringBootTest(
  properties = [
    "tolgee.cache.enabled=true",
    "tolgee.batch.concurrency=4",
  ],
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
)
class BatchJobConcurrentFinalizationTest : AbstractSpringTest() {
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
  lateinit var batchJobConcurrentLauncher: BatchJobConcurrentLauncher

  @Autowired
  lateinit var finalizedRecorder: FinalizedRecorder

  @BeforeEach
  fun setup() {
    finalizedRecorder.events.clear()
    testData = BatchJobsTestData()
    testData.addTranslationOperationData(200)
    testDataService.saveTestData(testData.root)
    setForcedDate(Date(1687237928000))
    batchJobConcurrentLauncher.pause = false
  }

  @AfterEach
  fun teardown() {
    clearForcedDate()
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `finalizes a multi-chunk job whose chunks run concurrently`() {
    val job = BatchJobTestUtil(applicationContext, testData).runChunkedJob(200)
    job.totalChunks.assert.isGreaterThan(1)

    waitForNotThrowing(pollTime = 200, timeout = 120_000) {
      executeInNewTransaction(platformTransactionManager) {
        batchJobService
          .getJobDtoNoCache(job.id)
          .status.assert
          .isEqualTo(BatchJobStatus.SUCCESS)
      }
    }

    waitForNotThrowing(pollTime = 200, timeout = 60_000) {
      finalizedRecorder.events
        .map { it.job.id }
        .assert
        .contains(job.id)
    }
  }
}
