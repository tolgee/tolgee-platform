package io.tolgee.batch

import io.tolgee.AbstractSpringTest
import io.tolgee.batch.processors.DeleteKeysChunkProcessor
import io.tolgee.batch.processors.PreTranslationByTmChunkProcessor
import io.tolgee.batch.request.DeleteKeysRequest
import io.tolgee.batch.request.PreTranslationByTmRequest
import io.tolgee.component.CurrentDateProvider
import io.tolgee.constants.Message
import io.tolgee.development.testDataBuilder.data.BatchJobsTestData
import io.tolgee.exceptions.OutOfCreditsException
import io.tolgee.fixtures.waitFor
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.model.batch.BatchJob
import io.tolgee.model.batch.BatchJobChunkExecution
import io.tolgee.model.batch.BatchJobChunkExecutionStatus
import io.tolgee.model.batch.BatchJobStatus
import io.tolgee.security.JwtTokenProvider
import io.tolgee.testing.WebsocketTest
import io.tolgee.testing.assert
import io.tolgee.util.Logging
import io.tolgee.util.addSeconds
import io.tolgee.util.logger
import io.tolgee.websocket.WebsocketTestHelper
import kotlinx.coroutines.ensureActive
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.atLeast
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.annotation.DirtiesContext
import java.util.*
import kotlin.coroutines.CoroutineContext
import kotlin.math.ceil

@WebsocketTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
abstract class AbstractBatchJobsGeneralTest : AbstractSpringTest(), Logging {

  private lateinit var testData: BatchJobsTestData

  @Autowired
  lateinit var batchJobService: BatchJobService

  @MockBean
  @Autowired
  lateinit var preTranslationByTmChunkProcessor: PreTranslationByTmChunkProcessor

  @MockBean
  @Autowired
  lateinit var deleteKeysChunkProcessor: DeleteKeysChunkProcessor

  @Autowired
  lateinit var batchJobActionService: BatchJobActionService

  @Autowired
  lateinit var jwtTokenProvider: JwtTokenProvider

  @LocalServerPort
  private val port: Int? = null

  lateinit var websocketHelper: WebsocketTestHelper

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

  @BeforeEach
  fun setup() {
    Mockito.reset(batchJobProjectLockingManager)
    Mockito.reset(progressManager)
    batchJobChunkExecutionQueue.clear()
    Mockito.reset(preTranslationByTmChunkProcessor)
    Mockito.clearInvocations(preTranslationByTmChunkProcessor)
    whenever(preTranslationByTmChunkProcessor.getParams(any<PreTranslationByTmRequest>())).thenCallRealMethod()
    whenever(preTranslationByTmChunkProcessor.getJobCharacter()).thenCallRealMethod()
    whenever(preTranslationByTmChunkProcessor.getMaxPerJobConcurrency()).thenCallRealMethod()
    whenever(preTranslationByTmChunkProcessor.getChunkSize(any(), any())).thenCallRealMethod()
    whenever(preTranslationByTmChunkProcessor.getTarget(any())).thenCallRealMethod()
    whenever(preTranslationByTmChunkProcessor.getTargetItemType()).thenCallRealMethod()
    whenever(deleteKeysChunkProcessor.getParams(any<DeleteKeysRequest>())).thenCallRealMethod()
    whenever(deleteKeysChunkProcessor.getTarget(any())).thenCallRealMethod()
    whenever(deleteKeysChunkProcessor.getJobCharacter()).thenCallRealMethod()
    whenever(deleteKeysChunkProcessor.getMaxPerJobConcurrency()).thenCallRealMethod()
    whenever(deleteKeysChunkProcessor.getChunkSize(any(), any())).thenCallRealMethod()
    whenever(deleteKeysChunkProcessor.getTargetItemType()).thenCallRealMethod()

    batchJobChunkExecutionQueue.populateQueue()
    testData = BatchJobsTestData()
    testDataService.saveTestData(testData.root)
    currentDateProvider.forcedDate = Date(1687237928000)
    initWebsocketHelper()
  }

  @AfterEach()
  fun teardown() {
    batchJobChunkExecutionQueue.clear()
    currentDateProvider.forcedDate = null
    websocketHelper.stop()
  }

  @Test
  fun `executes operation`() {
    val job = runChunkedJob(1000)

    job.totalItems.assert.isEqualTo(1000)

    waitForNotThrowing(pollTime = 1000) {
      verify(
        preTranslationByTmChunkProcessor,
        times(ceil(job.totalItems.toDouble() / 10).toInt())
      ).process(any(), any(), any(), any())
    }

    job.waitForCompleted().status.assert.isEqualTo(BatchJobStatus.SUCCESS)

    // 100 progress messages + 1 finish message
    websocketHelper.receivedMessages.assert.hasSize(101)
  }

  @Test
  fun `correctly reports failed test when FailedDontRequeueException thrown`() {
    val job = runChunkedJob(1000)

    val exceptions = (1..50).map { _ ->
      FailedDontRequeueException(
        Message.OUT_OF_CREDITS,
        cause = OutOfCreditsException(OutOfCreditsException.Reason.OUT_OF_CREDITS),
        successfulTargets = listOf()
      )
    }

    whenever(
      preTranslationByTmChunkProcessor.process(
        any(), any(), any(), any()
      )
    )
      .thenThrow(*exceptions.toTypedArray())
      .then {}

    waitForNotThrowing(pollTime = 1000) {
      executeInNewTransaction {
        val finishedJob = batchJobService.getJobDto(job.id)
        finishedJob.status.assert.isEqualTo(BatchJobStatus.FAILED)
      }
    }

    // 100 progress messages + 1 finish message
    websocketHelper.receivedMessages.assert.hasSize(51)
    assertStatusReported(BatchJobStatus.FAILED)
    assertMessagesContain("out_of_credits")

    waitForNotThrowing {
      executeInNewTransaction {
        batchJobService.getView(job.id).errorMessage.assert.isEqualTo(Message.OUT_OF_CREDITS)
      }
    }
  }

  @Test
  fun `retries failed with generic exception`() {
    whenever(
      preTranslationByTmChunkProcessor.process(
        any(),
        argThat { this.containsAll((1L..10).toList()) },
        any(),
        any()
      )
    ).thenThrow(RuntimeException("OMG! It failed"))

    val job = runChunkedJob(1000)

    (1..3).forEach {
      waitForNotThrowing {
        batchJobChunkExecutionQueue.find { it.executeAfter == currentDateProvider.date.time + 2000 }.assert.isNotNull
      }
      currentDateProvider.forcedDate = Date(currentDateProvider.date.time + 2000)
    }

    waitForNotThrowing(pollTime = 1000) {
      executeInNewTransaction {
        val finishedJob = batchJobService.getJobDto(job.id)
        val finishedJobEntity = batchJobService.getJobEntity(job.id)
        finishedJobEntity.status.assert.isEqualTo(BatchJobStatus.FAILED)
        finishedJob.status.assert.isEqualTo(BatchJobStatus.FAILED)
      }
    }

    entityManager.createQuery("""from BatchJobChunkExecution b where b.batchJob.id = :id""")
      .setParameter("id", job.id).resultList.assert.hasSize(103)

    // 100 progress messages + 1 finish message
    websocketHelper.receivedMessages.assert.hasSize(100)
    assertStatusReported(BatchJobStatus.FAILED)
  }

  @Test
  fun `retries failed with RequeueWithTimeoutException`() {
    val throwingChunk = (1L..10).toList()

    whenever(
      preTranslationByTmChunkProcessor.process(
        any(),
        argThat { this.containsAll(throwingChunk) },
        any(),
        any()
      )
    ).thenThrow(
      RequeueWithDelayException(
        message = Message.OUT_OF_CREDITS,
        successfulTargets = listOf(),
        cause = OutOfCreditsException(OutOfCreditsException.Reason.OUT_OF_CREDITS),
        delayInMs = 100,
        increaseFactor = 10,
        maxRetries = 3
      )
    )

    val job = runChunkedJob(1000)

    waitForNotThrowing {
      batchJobChunkExecutionQueue.find { it.executeAfter == currentDateProvider.date.time + 100 }.assert.isNotNull
    }

    currentDateProvider.forcedDate = Date(currentDateProvider.date.time + 100)

    waitForNotThrowing {
      batchJobChunkExecutionQueue.find { it.executeAfter == currentDateProvider.date.time + 1000 }.assert.isNotNull
    }

    currentDateProvider.forcedDate = Date(currentDateProvider.date.time + 1000)

    waitForNotThrowing {
      batchJobChunkExecutionQueue.find { it.executeAfter == currentDateProvider.date.time + 10000 }.assert.isNotNull
    }

    currentDateProvider.forcedDate = Date(currentDateProvider.date.time + 10000)

    waitForNotThrowing(pollTime = 1000) {
      executeInNewTransaction {
        val finishedJob = batchJobService.getJobDto(job.id)
        finishedJob.status.assert.isEqualTo(BatchJobStatus.FAILED)
      }
    }

    entityManager.createQuery("""from BatchJobChunkExecution b where b.batchJob.id = :id""")
      .setParameter("id", job.id).resultList.assert.hasSize(103)

    // 100 progress messages + 1 finish message
    websocketHelper.receivedMessages.assert.hasSize(100)
    assertStatusReported(BatchJobStatus.FAILED)
  }

  private fun initWebsocketHelper() {
    websocketHelper = WebsocketTestHelper(
      port,
      jwtTokenProvider.generateToken(testData.user.id).toString(),
      testData.projectBuilder.self.id
    )
    websocketHelper.listenForBatchJobProgress()
  }

  @Test
  fun `publishes progress of single chunk job`() {
    whenever(
      deleteKeysChunkProcessor.process(
        any(),
        any(),
        any(),
        any()
      )
    ).thenAnswer {
      @Suppress("UNCHECKED_CAST")
      val chunk = it.arguments[1] as List<Long>

      @Suppress("UNCHECKED_CAST")
      val onProgress = it.arguments[3] as ((progress: Int) -> Unit)

      chunk.forEachIndexed { index, _ ->
        onProgress(index + 1)
      }
    }

    val job = runSingleChunkJob(100)

    job.waitForCompleted().status.assert.isEqualTo(BatchJobStatus.SUCCESS)

    waitForNotThrowing(pollTime = 1000) {
      entityManager.createQuery("""from BatchJobChunkExecution b where b.batchJob.id = :id""")
        .setParameter("id", job.id).resultList.assert.hasSize(1)
    }
    waitForNotThrowing {
      // 100 progress messages + 1 finish message
      websocketHelper.receivedMessages.assert.hasSize(101)
      assertStatusReported(BatchJobStatus.SUCCESS)
    }
  }

  private fun assertStatusReported(status: BatchJobStatus) {
    assertMessagesContain(status.name)
  }

  private fun assertMessagesContain(string: String) {
    websocketHelper.receivedMessages.assert.anyMatch { it.contains(string) }
  }

  @Test
  fun `cancels the job`() {
    var count = 0

    whenever(preTranslationByTmChunkProcessor.process(any(), any(), any(), any())).then {
      if (count++ > 50) {
        while (true) {
          val context = it.arguments[2] as CoroutineContext
          context.ensureActive()
          Thread.sleep(100)
        }
      }
    }

    val job = runChunkedJob(1000)

    waitFor {
      count > 50
    }

    batchJobCancellationManager.cancel(job.id)

    job.waitForCompleted().status.assert.isEqualTo(BatchJobStatus.CANCELLED)

    websocketHelper.receivedMessages.assert.hasSizeGreaterThan(49)
    websocketHelper.receivedMessages.last.contains("CANCELLED")
  }

  @Test
  fun `it locks the single job for project`() {
    logger.info("Running test: it locks the single job for project")
    currentDateProvider.forcedDate = null
    batchJobConcurrentLauncher.pause = true

    val job1 = runChunkedJob(20)
    val job2 = runChunkedJob(20)

    val executions = getExecutions(listOf(job1.id, job2.id))

    val firstExecution = executions[job1.id]!!.first()
    val secondExecution = executions[job2.id]!!.first()
    val thirdExecution = executions[job1.id]!![1]
    val fourthExecution = executions[job2.id]!![1]

    batchJobChunkExecutionQueue.clear()

    batchJobConcurrentLauncher.pause = false

    batchJobChunkExecutionQueue.addToQueue(listOf(firstExecution))

    waitFor(pollTime = 1000) {
      batchJobService.getExecution(firstExecution.id).status == BatchJobChunkExecutionStatus.SUCCESS
    }
    // The first job is now locked
    batchJobProjectLockingManager.getLockedForProject(testData.projectBuilder.self.id).assert.isEqualTo(job1.id)

    batchJobChunkExecutionQueue.addToQueue(listOf(secondExecution))

    waitForNotThrowing {
      // it tries to lock the second job but it can't since the first job is locked
      verify(batchJobProjectLockingManager, atLeast(1))
        .canRunBatchJobOfExecution(eq(job2.id))
    }

    // it doesn't run the second execution since the first job is locked
    Thread.sleep(1000)
    batchJobService.getExecution(secondExecution.id).status.assert.isEqualTo(BatchJobChunkExecutionStatus.PENDING)

    batchJobChunkExecutionQueue.addToQueue(listOf(thirdExecution))

    // second and last execution of job1 is done, so the second job is locked now
    waitFor(pollTime = 1000) {
      batchJobService.getExecution(thirdExecution.id).status == BatchJobChunkExecutionStatus.SUCCESS
    }

    job1.waitForCompleted().status.assert.isEqualTo(BatchJobStatus.SUCCESS)

    waitForNotThrowing {
      // the project was unlocked before job2 acquired the job
      verify(batchJobProjectLockingManager, times(1)).unlockJobForProject(eq(job1.project.id))
    }

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
  }

  @Test
  /**
   * the chunk processing status is stored in the database in the same transaction
   * so when it fails on some management processing issue, we need to handle this
   *
   * The execution itself is added back to queue and retried, but we don't want it to be retreied forever.
   * That's why there is a limit of few retries per chunk
   *
   * This test tests that the job fails after few retries
   */
  fun `retries and fails on management exceptions issues`() {
    whenever(preTranslationByTmChunkProcessor.process(any(), any(), any(), any())).then {}

    val keyCount = 100
    val job = runChunkedJob(keyCount)

    doThrow(RuntimeException("test")).whenever(progressManager)
      .handleProgress(argThat { this.status != BatchJobChunkExecutionStatus.FAILED })

    waitForNotThrowing(pollTime = 100) {
      currentDateProvider.forcedDate = currentDateProvider.date.addSeconds(2)
      executeInNewTransaction {
        val finishedJob = batchJobService.getJobDto(job.id)
        finishedJob.status.assert.isEqualTo(BatchJobStatus.FAILED)
      }
    }

    // verify it retreied, so the processor was executed multiple times
    val totalChunks = keyCount / 10
    val totalRetries = 3
    verify(preTranslationByTmChunkProcessor, times((totalRetries + 1) * totalChunks)).process(
      any(),
      any(),
      any(),
      any()
    )

    waitForNotThrowing {
      assertStatusReported(BatchJobStatus.FAILED)
    }
  }

  private fun BatchJob.waitForCompleted(): BatchJobDto {
    waitForNotThrowing(pollTime = 1000) {
      executeInNewTransaction {
        val finishedJob = batchJobService.getJobDto(this.id)
        finishedJob.status.completed.assert.isTrue()
      }
    }
    return batchJobService.getJobDto(this.id)
  }

  private fun getExecutions(
    jobIds: List<Long>,
  ): Map<Long, List<BatchJobChunkExecution>> =
    entityManager.createQuery(
      """from BatchJobChunkExecution b left join fetch b.batchJob where b.batchJob.id in :ids""",
      BatchJobChunkExecution::class.java
    )
      .setParameter("ids", jobIds).resultList.groupBy { it.batchJob.id }

  fun runChunkedJob(keyCount: Int): BatchJob {
    return executeInNewTransaction {
      batchJobService.startJob(
        request = PreTranslationByTmRequest().apply {
          keyIds = (1L..keyCount).map { it }
        },
        project = testData.projectBuilder.self,
        author = testData.user,
        type = BatchJobType.PRE_TRANSLATE_BY_MT
      )
    }
  }

  protected fun runSingleChunkJob(keyCount: Int): BatchJob {
    return executeInNewTransaction {
      batchJobService.startJob(
        request = DeleteKeysRequest().apply {
          keyIds = (1L..keyCount).map { it }
        },
        project = testData.projectBuilder.self,
        author = testData.user,
        type = BatchJobType.DELETE_KEYS
      )
    }
  }
}
