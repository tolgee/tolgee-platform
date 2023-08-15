package io.tolgee.batch

import io.tolgee.AbstractSpringTest
import io.tolgee.batch.processors.DeleteKeysChunkProcessor
import io.tolgee.batch.processors.PreTranslationByTmChunkProcessor
import io.tolgee.batch.state.BatchJobStateProvider
import io.tolgee.component.CurrentDateProvider
import io.tolgee.constants.Message
import io.tolgee.development.testDataBuilder.data.BatchJobsTestData
import io.tolgee.fixtures.waitFor
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.model.batch.BatchJobChunkExecutionStatus
import io.tolgee.model.batch.BatchJobStatus
import io.tolgee.security.JwtTokenProvider
import io.tolgee.testing.WebsocketTest
import io.tolgee.testing.assert
import io.tolgee.util.Logging
import io.tolgee.util.logger
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito
import org.mockito.kotlin.atLeast
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.SpyBean
import java.time.Duration
import java.util.*
import kotlin.math.ceil

@WebsocketTest
abstract class AbstractBatchJobsGeneralTest : AbstractSpringTest(), Logging {

  private lateinit var testData: BatchJobsTestData

  @Autowired
  lateinit var batchJobService: BatchJobService

  @SpyBean
  @Autowired
  lateinit var preTranslationByTmChunkProcessor: PreTranslationByTmChunkProcessor

  @SpyBean
  @Autowired
  lateinit var deleteKeysChunkProcessor: DeleteKeysChunkProcessor

  @Autowired
  lateinit var batchJobActionService: BatchJobActionService

  @Autowired
  lateinit var jwtTokenProvider: JwtTokenProvider

  @Autowired
  lateinit var currentDateProvider: CurrentDateProvider

  @Autowired
  lateinit var batchJobCancellationManager: BatchJobCancellationManager

  @Autowired
  lateinit var batchJobChunkExecutionQueue: BatchJobChunkExecutionQueue

  @Autowired
  lateinit var batchJobConcurrentLauncher: BatchJobConcurrentLauncher

  @Autowired
  @SpyBean
  lateinit var batchJobProjectLockingManager: BatchJobProjectLockingManager

  @Autowired
  @SpyBean
  lateinit var progressManager: ProgressManager

  @Autowired
  lateinit var batchJobStateProvider: BatchJobStateProvider

  @Autowired
  lateinit var cachingBatchJobService: CachingBatchJobService

  lateinit var util: BatchJobTestUtil

  @BeforeEach
  fun setup() {
    Mockito.reset(batchJobProjectLockingManager)
    Mockito.reset(progressManager)
    batchJobChunkExecutionQueue.clear()
    Mockito.reset(preTranslationByTmChunkProcessor)
    Mockito.clearInvocations(preTranslationByTmChunkProcessor)
    batchJobChunkExecutionQueue.populateQueue()

    testData = BatchJobsTestData()
    testDataService.saveTestData(testData.root)
    util = BatchJobTestUtil(applicationContext, testData)

    currentDateProvider.forcedDate = Date(1687237928000)
    util.initWebsocketHelper()
    batchJobConcurrentLauncher.pause = false
  }

  @AfterEach()
  fun teardown() {
    batchJobChunkExecutionQueue.clear()
    batchJobConcurrentLauncher.pause = true
    currentDateProvider.forcedDate = null
    util.websocketHelper.stop()
  }

  @Test
  fun `executes operation`() {
    val job = util.runChunkedJob(1000)
    job.totalItems.assert.isEqualTo(1000)
    util.assertPreTranslationProcessExecutedTimes(ceil(job.totalItems.toDouble() / 10).toInt())
    util.waitForCompleted(job).status.assert.isEqualTo(BatchJobStatus.SUCCESS)
    util.assertTotalWebsocketMessagesCount(101)
  }

  @Test
  fun `correctly reports failed test when FailedDontRequeueException thrown`() {
    val job = util.runChunkedJob(1000)
    util.makePreTranslateProcessorThrowOutOfCreditsTimes(50)
    util.waitForJobFailed(job)
    util.assertTotalWebsocketMessagesCount(51)
    util.assertMessagesContain("out_of_credits")
    util.assertJobFailedWithMessage(job, Message.OUT_OF_CREDITS)
    util.assertJobStateCacheCleared(job)
  }

  @Test
  fun `retries failed with generic exception`() {
    util.makePreTranslateProcessorThrowGenericException()
    val job = util.runChunkedJob(1000)
    util.verifyConstantRepeats(3, 2000)
    util.waitForJobFailed(job)
    util.assertTotalExecutionsCount(job, 103)
    util.assertTotalWebsocketMessagesCount(100)
    util.assertStatusReported(BatchJobStatus.FAILED)
    util.assertJobStateCacheCleared(job)
  }

  @Test
  fun `retrying works with error keys`() {
    util.makeDeleteKeysProcessorThrowDifferentGenericExceptions()

    val job = util.runSingleChunkJob(100)

    util.fastForwardToFailedJob(job)

    val executions = batchJobService.getExecutions(job.id)
    val errorKeys = executions.sortedBy { it.id }.map { it.errorKey }

    // the retry count is computed for each error key separately, root cause exception is used
    // as the error key by default
    errorKeys.count { it == "IllegalStateException" }.assert.isEqualTo(4)
    errorKeys.count { it == "NotFoundException" }.assert.isEqualTo(2)
    errorKeys.count { it == "RuntimeException" }.assert.isEqualTo(2)
    util.assertJobStateCacheCleared(job)
  }

  @Test
  fun `retries failed with RequeueWithTimeoutException`() {
    util.makePreTranslateProcessorRepeatedlyThrowRequeueException()

    val job = util.runChunkedJob(1000)

    util.waitForRetryExecutionCreated(100)
    currentDateProvider.move(Duration.ofMillis(100))
    util.waitForRetryExecutionCreated(1000)
    currentDateProvider.move(Duration.ofMillis(1000))
    util.waitForRetryExecutionCreated(10000)
    currentDateProvider.move(Duration.ofMillis(10000))

    util.waitForJobFailed(job)
    util.assertTotalExecutionsCount(job, 103)
    util.assertTotalWebsocketMessagesCount(100)
    util.assertStatusReported(BatchJobStatus.FAILED)
    util.assertJobStateCacheCleared(job)
  }

  @Test
  fun `publishes progress of single chunk job`() {
    util.makeDeleteChunkProcessorReportProgressOnEachItem()
    val job = util.runSingleChunkJob(100)

    util.waitForCompleted(job).status.assert.isEqualTo(BatchJobStatus.SUCCESS)
    util.assertTotalExecutionsCount(job, 1)
    util.assertTotalWebsocketMessagesCount(101)
    util.assertStatusReported(BatchJobStatus.SUCCESS)
    util.assertJobStateCacheCleared(job)
  }

  @Test
  fun `cancels the job`() {
    val waitFor50Executions = util.makePreTranslationProcessorWaitingAfter50Executions()
    val job = util.runChunkedJob(1000)
    waitFor50Executions()

    batchJobCancellationManager.cancel(job.id)

    util.waitForJobCancelled(job)
    util.assertMessagesContain("CANCELLED")
    util.assertTotalWebsocketMessagesCountGreaterThan(49)
    util.assertJobStateCacheCleared(job)
    util.assertJobUnlocked()
  }

  @Test
  fun `it locks the single job for project`() {
    logger.info("Running test: it locks the single job for project")
    currentDateProvider.forcedDate = null

    logger.debug("Pausing the job launcher")
    batchJobConcurrentLauncher.pause = true

    logger.debug("Running the jobs")
    val job1 = util.runChunkedJob(20)
    val job2 = util.runChunkedJob(20)

    val executions = util.getExecutions(listOf(job1.id, job2.id))

    val firstExecution = executions[job1.id]!!.first()
    val secondExecution = executions[job2.id]!!.first()
    val thirdExecution = executions[job1.id]!![1]
    val fourthExecution = executions[job2.id]!![1]

    waitForNotThrowing(pollTime = 50, timeout = 2000) {
      batchJobChunkExecutionQueue.size.assert.isEqualTo(4)
    }

    logger.debug("Clearing queue")
    batchJobChunkExecutionQueue.clear()

    batchJobChunkExecutionQueue.size.assert.isEqualTo(0)

    logger.debug("Starting the job launcher")
    batchJobConcurrentLauncher.pause = false

    logger.debug("Adding the first execution to the queue $firstExecution")
    batchJobChunkExecutionQueue.addToQueue(listOf(firstExecution))

    logger.debug("Waiting for the first execution to finish")
    waitFor(pollTime = 1000) {
      batchJobService.getExecution(firstExecution.id).status == BatchJobChunkExecutionStatus.SUCCESS
    }

    logger.debug("Verify job is locked")
    // The first job is now locked
    batchJobProjectLockingManager.getLockedForProject(testData.projectBuilder.self.id).assert.isEqualTo(job1.id)

    logger.debug("Adding the second execution to the queue $secondExecution")
    batchJobChunkExecutionQueue.addToQueue(listOf(secondExecution))

    logger.debug("Verifies it tries to acquire the second job but it can't since the first job is locked")
    waitForNotThrowing {
      // it tries to lock the second job but it can't since the first job is locked
      verify(batchJobProjectLockingManager, atLeast(1))
        .canRunBatchJobOfExecution(eq(job2.id))
    }

    logger.debug("Asserts the second job is still pending")
    // it doesn't run the second execution since the first job is locked
    Thread.sleep(1000)
    batchJobService.getExecution(secondExecution.id).status.assert.isEqualTo(BatchJobChunkExecutionStatus.PENDING)

    logger.debug("Adding the third execution to the queue $thirdExecution")
    batchJobChunkExecutionQueue.addToQueue(listOf(thirdExecution))

    logger.debug("Verify the third execution is successful")
    // second and last execution of job1 is done, so the second job is locked now
    waitFor(pollTime = 1000) {
      batchJobService.getExecution(thirdExecution.id).status == BatchJobChunkExecutionStatus.SUCCESS
    }

    logger.debug("Verify the first job is successful")
    util.waitForJobSuccess(job1)

    logger.debug("Verify it unlocks the first job")
    waitForNotThrowing {
      // the project was unlocked before job2 acquired the job
      verify(batchJobProjectLockingManager, times(1)).unlockJobForProject(eq(job1.project.id), eq(job1.id))
    }

    logger.debug("Verify the second job is locked")
    waitForNotThrowing(pollTime = 1000) {
      batchJobProjectLockingManager.getLockedForProject(testData.projectBuilder.self.id).assert.isEqualTo(job2.id)
    }

    batchJobChunkExecutionQueue.addToQueue(listOf(fourthExecution))

    waitFor(pollTime = 1000) {
      batchJobService.getExecution(fourthExecution.id).status == BatchJobChunkExecutionStatus.SUCCESS &&
        batchJobProjectLockingManager.getLockedForProject(testData.projectBuilder.self.id) == 0L
    }

    batchJobService.getJobDto(job1.id).status.assert.isEqualTo(BatchJobStatus.SUCCESS)
    batchJobService.getJobDto(job2.id).status.assert.isEqualTo(BatchJobStatus.SUCCESS)
    util.assertJobStateCacheCleared(job1)
    util.assertJobStateCacheCleared(job2)
    util.assertJobUnlocked()
  }

  /**
   * the chunk processing status is stored in the database in the same transaction
   * so when it fails on some management processing issue, we need to handle this
   *
   * The execution itself is added back to queue and retried, but we don't want it to be retreied forever.
   * That's why there is a limit of few retries per chunk
   *
   * This test tests that the job fails after few retries
   */
  @Test
  fun `retries and fails on management exceptions issues`() {
    util.makePreTranslateProcessorPass()

    val keyCount = 100
    val job = util.runChunkedJob(keyCount)

    util.makeProgressManagerFail()

    util.fastForwardToFailedJob(job)

    val totalChunks = keyCount / 10
    val totalRetries = 10

    util.assertPreTranslationProcessExecutedTimes(totalChunks * (totalRetries + 1))
    util.assertStatusReported(BatchJobStatus.FAILED)
    util.assertJobStateCacheCleared(job)
    util.assertJobUnlocked()
  }
}
