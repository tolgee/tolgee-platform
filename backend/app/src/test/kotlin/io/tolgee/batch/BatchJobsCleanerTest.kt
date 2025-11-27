package io.tolgee.batch

import io.tolgee.AbstractSpringTest
import io.tolgee.development.testDataBuilder.data.BaseTestData
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.model.batch.BatchJobChunkExecutionStatus
import io.tolgee.model.batch.BatchJobStatus
import io.tolgee.testing.ContextRecreatingTest
import io.tolgee.testing.assert
import io.tolgee.util.StuckBatchJobTestUtil
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext

@SpringBootTest(
  properties = ["tolgee.batch.scheduled-handle-stuck-job-delay=200"],
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ContextRecreatingTest
class BatchJobsCleanerTest : AbstractSpringTest() {
  @Autowired
  lateinit var jobConcurrentLauncher: BatchJobConcurrentLauncher

  @Autowired
  lateinit var batchJobService: BatchJobService

  lateinit var testData: BaseTestData

  @Autowired
  lateinit var stuckBatchJobTestUtil: StuckBatchJobTestUtil

  @BeforeEach
  fun setup() {
    testData = BaseTestData()
    testDataService.saveTestData(testData.root)
  }

  @AfterEach
  fun cleanup() {
    jobConcurrentLauncher.pause = false
  }

  @Test
  fun `fixes the batch job state`() {
    val cancelledJob = createCancelledJob()
    val failedJob = createFailedJob()
    val successBatchJob = createSuccessBatchJob()
    val noExecutionBatchJob = createNoExecutionBatchJob()
    val runningJob = createRunningJob()
    val pendingJob = createPendingJob()
    val dontTouchJob = createDontTouchJob()
    val successWithRetriedChunks = createSuccessWithRetriedChunks()

    waitForNotThrowing(timeout = 2000, pollTime = 100) {
      batchJobService
        .getJobDto(cancelledJob.id)
        .status.assert
        .isEqualTo(BatchJobStatus.CANCELLED)
      batchJobService
        .getJobDto(failedJob.id)
        .status.assert
        .isEqualTo(BatchJobStatus.FAILED)
      batchJobService
        .getJobDto(successBatchJob.id)
        .status.assert
        .isEqualTo(BatchJobStatus.SUCCESS)
      batchJobService
        .getJobDto(noExecutionBatchJob.id)
        .status.assert
        .isEqualTo(BatchJobStatus.SUCCESS)
      batchJobService
        .getJobDto(runningJob.id)
        .status.assert
        .isEqualTo(BatchJobStatus.RUNNING)
      batchJobService
        .getJobDto(pendingJob.id)
        .status.assert
        .isEqualTo(BatchJobStatus.PENDING)
      batchJobService
        .getJobDto(dontTouchJob.id)
        .status.assert
        .isEqualTo(BatchJobStatus.FAILED)
      batchJobService
        .getJobDto(successWithRetriedChunks.id)
        .status.assert
        .isEqualTo(BatchJobStatus.SUCCESS)
    }
  }

  private fun createDontTouchJob() =
    stuckBatchJobTestUtil.createBatchJobWithExecutionStatuses(
      testData.project,
      BatchJobStatus.FAILED,
      setOf(BatchJobChunkExecutionStatus.CANCELLED),
    )

  private fun createPendingJob() =
    stuckBatchJobTestUtil.createBatchJobWithExecutionStatuses(
      testData.project,
      BatchJobStatus.PENDING,
      setOf(BatchJobChunkExecutionStatus.PENDING),
    )

  private fun createRunningJob() =
    stuckBatchJobTestUtil.createBatchJobWithExecutionStatuses(
      testData.project,
      BatchJobStatus.RUNNING,
      setOf(BatchJobChunkExecutionStatus.RUNNING),
    )

  private fun createNoExecutionBatchJob() =
    stuckBatchJobTestUtil.createBatchJobWithExecutionStatuses(
      testData.project,
      BatchJobStatus.RUNNING,
      setOf(),
    )

  private fun createSuccessBatchJob() =
    stuckBatchJobTestUtil.createBatchJobWithExecutionStatuses(
      testData.project,
      BatchJobStatus.RUNNING,
      setOf(
        BatchJobChunkExecutionStatus.SUCCESS,
      ),
    )

  private fun createCancelledJob() =
    stuckBatchJobTestUtil.createBatchJobWithExecutionStatuses(
      testData.project,
      BatchJobStatus.RUNNING,
      setOf(
        BatchJobChunkExecutionStatus.CANCELLED,
        BatchJobChunkExecutionStatus.FAILED,
        BatchJobChunkExecutionStatus.SUCCESS,
      ),
    )

  private fun createFailedJob() =
    stuckBatchJobTestUtil.createBatchJobWithExecutionStatuses(
      testData.project,
      BatchJobStatus.RUNNING,
      setOf(
        BatchJobChunkExecutionStatus.SUCCESS,
        BatchJobChunkExecutionStatus.FAILED,
      ),
    )

  private fun createSuccessWithRetriedChunks() =
    stuckBatchJobTestUtil.createBatchJobWithExecutionStatuses(
      testData.project,
      BatchJobStatus.RUNNING,
      mapOf(
        1 to
          listOf(
            BatchJobChunkExecutionStatus.FAILED,
            BatchJobChunkExecutionStatus.FAILED,
            BatchJobChunkExecutionStatus.SUCCESS,
          ),
      ),
    )
}
