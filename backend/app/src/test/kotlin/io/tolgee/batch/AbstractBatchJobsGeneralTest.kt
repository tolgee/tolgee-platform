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
import io.tolgee.model.batch.BatchJobChunkExecutionStatus
import io.tolgee.model.batch.BatchJobStatus
import io.tolgee.security.JwtTokenProvider
import io.tolgee.testing.WebsocketTest
import io.tolgee.testing.assert
import io.tolgee.util.addSeconds
import io.tolgee.websocket.WebsocketTestHelper
import kotlinx.coroutines.ensureActive
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
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
abstract class AbstractBatchJobsGeneralTest : AbstractSpringTest() {

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
  @SpyBean
  lateinit var progressManager: ProgressManager

  @BeforeEach
  fun setup() {
    Mockito.reset(progressManager)
    batchJobChunkExecutionQueue.clear()
    Mockito.reset(preTranslationByTmChunkProcessor)
    Mockito.clearInvocations(preTranslationByTmChunkProcessor)
    whenever(preTranslationByTmChunkProcessor.getParams(any<PreTranslationByTmRequest>())).thenCallRealMethod()
    whenever(preTranslationByTmChunkProcessor.getTarget(any())).thenCallRealMethod()
    whenever(deleteKeysChunkProcessor.getParams(any<DeleteKeysRequest>())).thenCallRealMethod()
    whenever(deleteKeysChunkProcessor.getTarget(any())).thenCallRealMethod()
    batchJobChunkExecutionQueue.populateQueue()
    testData = BatchJobsTestData()
    testDataService.saveTestData(testData.root)
    currentDateProvider.forcedDate = Date(1687237928000)
    websocketHelper = WebsocketTestHelper(
      port,
      jwtTokenProvider.generateToken(testData.user.id).toString(),
      testData.projectBuilder.self.id
    )
    websocketHelper.listenForBatchJobProgress()
  }

  @AfterEach()
  fun teardown() {
    batchJobChunkExecutionQueue.clear()
    currentDateProvider.forcedDate = null
    websocketHelper.stop()
  }

  @Test
  fun `executes operation`() {
    websocketHelper = WebsocketTestHelper(
      port,
      jwtTokenProvider.generateToken(testData.user.id).toString(),
      testData.projectBuilder.self.id
    )
    websocketHelper.listenForBatchJobProgress()

    val job = runChunkedJob(1000)

    job.totalItems.assert.isEqualTo(1000)

    waitForNotThrowing(pollTime = 1000) {
      verify(
        preTranslationByTmChunkProcessor,
        times(ceil(job.totalItems.toDouble() / BatchJobType.PRE_TRANSLATE_BY_MT.chunkSize).toInt())
      ).process(any(), any(), any(), any())
    }

    waitForNotThrowing(pollTime = 1000) {
      executeInNewTransaction {
        val finishedJob = batchJobService.getJobDto(job.id)
        finishedJob.status.assert.isEqualTo(BatchJobStatus.SUCCESS)
      }
    }

    // 100 progress messages + 1 finish message
    websocketHelper.receivedMessages.assert.hasSize(101)
  }

  @Test
  fun `correctly reports failed test when FailedDontRequeueException thrown`() {
    websocketHelper = WebsocketTestHelper(
      port,
      jwtTokenProvider.generateToken(testData.user.id).toString(),
      testData.projectBuilder.self.id
    )
    websocketHelper.listenForBatchJobProgress()

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
    websocketHelper = WebsocketTestHelper(
      port,
      jwtTokenProvider.generateToken(testData.user.id).toString(),
      testData.projectBuilder.self.id
    )
    websocketHelper.listenForBatchJobProgress()

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
    websocketHelper = WebsocketTestHelper(
      port,
      jwtTokenProvider.generateToken(testData.user.id).toString(),
      testData.projectBuilder.self.id
    )
    websocketHelper.listenForBatchJobProgress()

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

    waitForNotThrowing(pollTime = 1000) {
      executeInNewTransaction {
        val finishedJob = batchJobService.getJobDto(job.id)
        finishedJob.status.assert.isEqualTo(BatchJobStatus.SUCCESS)
      }

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

    waitForNotThrowing(pollTime = 1000) {
      executeInNewTransaction {
        val finishedJob = batchJobService.getJobDto(job.id)
        finishedJob.status.assert.isEqualTo(BatchJobStatus.CANCELLED)
      }
    }

    websocketHelper.receivedMessages.assert.hasSizeGreaterThan(49)
    assertStatusReported(BatchJobStatus.CANCELLED)
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

  protected fun runChunkedJob(keyCount: Int): BatchJob {
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
