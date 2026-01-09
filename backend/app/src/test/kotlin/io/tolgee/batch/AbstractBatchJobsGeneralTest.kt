package io.tolgee.batch

import io.tolgee.AbstractSpringTest
import io.tolgee.batch.processors.AutomationChunkProcessor
import io.tolgee.batch.processors.DeleteKeysChunkProcessor
import io.tolgee.batch.processors.PreTranslationByTmChunkProcessor
import io.tolgee.configuration.tolgee.BatchProperties
import io.tolgee.constants.Message
import io.tolgee.development.testDataBuilder.data.BatchJobsTestData
import io.tolgee.model.batch.BatchJobStatus
import io.tolgee.testing.WebsocketTest
import io.tolgee.testing.assert
import io.tolgee.util.Logging
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import java.time.Duration
import java.util.Date
import kotlin.math.ceil

@WebsocketTest
abstract class AbstractBatchJobsGeneralTest :
  AbstractSpringTest(),
  Logging {
  private lateinit var testData: BatchJobsTestData

  @Autowired
  lateinit var batchJobService: BatchJobService

  @Autowired
  lateinit var batchProperties: BatchProperties

  @MockitoSpyBean
  @Autowired
  lateinit var preTranslationByTmChunkProcessor: PreTranslationByTmChunkProcessor

  @Suppress("unused") // Used to instrument it in other places via @MockitoSpyBean
  @MockitoSpyBean
  @Autowired
  lateinit var deleteKeysChunkProcessor: DeleteKeysChunkProcessor

  @Autowired
  lateinit var batchJobActionService: BatchJobActionService

  @Autowired
  lateinit var batchJobCancellationManager: BatchJobCancellationManager

  @Autowired
  lateinit var batchJobChunkExecutionQueue: BatchJobChunkExecutionQueue

  @Autowired
  lateinit var batchJobConcurrentLauncher: BatchJobConcurrentLauncher

  @Autowired
  @MockitoSpyBean
  lateinit var batchJobProjectLockingManager: BatchJobProjectLockingManager

  @Autowired
  @MockitoSpyBean
  lateinit var automationChunkProcessor: AutomationChunkProcessor

  @Autowired
  @MockitoSpyBean
  lateinit var progressManager: ProgressManager

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

    setForcedDate(Date(1687237928000))
    util.initWebsocketHelper()
    batchJobConcurrentLauncher.pause = false
  }

  @AfterEach
  fun teardown() {
    batchJobChunkExecutionQueue.clear()
    batchJobConcurrentLauncher.pause = true
    clearForcedDate()
    util.websocketHelper.stop()
  }

  @Test
  fun `executes operation`() {
    val job = util.runChunkedJob(1000)
    job.totalItems.assert.isEqualTo(1000)
    util.assertPreTranslationProcessExecutedTimes(ceil(job.totalItems.toDouble() / 10).toInt())
    util
      .waitForCompleted(job)
      .status.assert
      .isEqualTo(BatchJobStatus.SUCCESS)
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
    moveCurrentDate(Duration.ofMillis(100))
    util.waitForRetryExecutionCreated(1000)
    moveCurrentDate(Duration.ofMillis(1000))
    util.waitForRetryExecutionCreated(10000)
    moveCurrentDate(Duration.ofMillis(10000))

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

    util
      .waitForCompleted(job)
      .status.assert
      .isEqualTo(BatchJobStatus.SUCCESS)
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
    clearForcedDate()
    batchJobConcurrentLauncher.pause = true

    val job1 = util.runChunkedJob(20)
    val job2 = util.runChunkedJob(20)

    val executions = util.getExecutions(listOf(job1.id, job2.id))

    val firstExecution = executions[job1.id]!!.first()
    val secondExecution = executions[job2.id]!!.first()
    val thirdExecution = executions[job1.id]!![1]
    val fourthExecution = executions[job2.id]!![1]

    util.waitAndClearQueue(4)

    batchJobChunkExecutionQueue.addToQueue(listOf(firstExecution))
    util.waitForExecutionSuccess(firstExecution)
    util.verifyJobLocked(job1)

    batchJobChunkExecutionQueue.addToQueue(listOf(secondExecution))
    util.verifiedTriedToLockJob(job2.id)
    // it doesn't run the second execution since the first job is locked
    Thread.sleep(200)
    util.verifyExecutionPending(secondExecution)

    batchJobChunkExecutionQueue.addToQueue(listOf(thirdExecution))
    util.waitForExecutionSuccess(thirdExecution)
    util.waitForJobSuccess(job1)
    util.verifyJobUnlocked(job1)
    util.verifyJobLocked(job2)

    batchJobChunkExecutionQueue.addToQueue(listOf(fourthExecution))
    util.waitForExecutionSuccess(fourthExecution)
    util.verifyProjectJobLockReleased()

    util.waitForJobSuccess(job1)
    util.waitForJobSuccess(job2)
    util.assertJobStateCacheCleared(job1)
    util.assertJobStateCacheCleared(job2)
    util.assertJobUnlocked()
  }

  @Test
  fun `doesn't lock non-exclusive job`() {
    clearForcedDate()
    batchJobConcurrentLauncher.pause = true

    val job1 = util.runChunkedJob(20)
    val job2 = util.runNonExclusiveJob()

    val executions = util.getExecutions(listOf(job1.id, job2.id))

    val firstExecution = executions[job1.id]!!.first()
    val secondExecution = executions[job2.id]!!.first()

    util.waitAndClearQueue(3)

    batchJobChunkExecutionQueue.addToQueue(listOf(firstExecution))
    util.waitForExecutionSuccess(firstExecution)
    util.verifyJobLocked(job1)

    batchJobChunkExecutionQueue.addToQueue(listOf(secondExecution))
    util.waitForExecutionSuccess(secondExecution)
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

  @Test
  fun `debounces job`() {
    currentDateProvider.forcedDate = currentDateProvider.date
    currentDateProvider.date

    util.makeAutomationChunkProcessorPass()
    val firstJobId = util.runDebouncedJob().id

    repeat(2) {
      Thread.sleep(500)
      util
        .runDebouncedJob()
        .id.assert
        .isEqualTo(firstJobId)
    }
    currentDateProvider.move(Duration.ofSeconds(5))

    Thread.sleep(500)
    repeat(2) {
      util
        .runDebouncedJob()
        .id.assert
        .isEqualTo(firstJobId)
    }
    currentDateProvider.move(Duration.ofSeconds(10))
    Thread.sleep(500)

    val anotherJobId = util.runDebouncedJob().id
    anotherJobId.assert.isNotEqualTo(firstJobId)

    // test it debounces for max time (10 sec * 4 = 40)
    repeat(7) {
      currentDateProvider.move(Duration.ofSeconds(2))
      Thread.sleep(20)
      util
        .runDebouncedJob()
        .id.assert
        .isEqualTo(anotherJobId)
      currentDateProvider.move(Duration.ofSeconds(3))
      Thread.sleep(20)
      util
        .runDebouncedJob()
        .id.assert
        .isEqualTo(anotherJobId)
    }

    currentDateProvider.move(Duration.ofSeconds(5))
    Thread.sleep(500)
    util
      .runDebouncedJob()
      .id.assert
      .isNotEqualTo(anotherJobId)
  }

  @Test
  fun `mt job respects maxPerJobConcurrency`() {
    val mtDefault = batchProperties.maxPerMtJobConcurrency
    val maxConcurrency = 1
    batchProperties.maxPerMtJobConcurrency = maxConcurrency

    try {
      val mtJob = util.runMtJob(100)
      util.assertAllowedMaxPerJobConcurrency(mtJob, maxConcurrency)
      util.assertMaxPerJobConcurrencyIsLessThanOrEqualTo(maxConcurrency)
      util.waitForJobSuccess(mtJob)
      util.assertJobUnlocked()
    } finally {
      batchProperties.maxPerMtJobConcurrency = mtDefault
    }
  }
}
