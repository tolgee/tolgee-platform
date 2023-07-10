package io.tolgee.batch

import io.tolgee.AbstractSpringTest
import io.tolgee.batch.processors.DeleteKeysChunkProcessor
import io.tolgee.batch.processors.TranslationChunkProcessor
import io.tolgee.batch.request.BatchTranslateRequest
import io.tolgee.batch.request.DeleteKeysRequest
import io.tolgee.component.CurrentDateProvider
import io.tolgee.constants.Message
import io.tolgee.development.testDataBuilder.data.BatchJobsTestData
import io.tolgee.exceptions.OutOfCreditsException
import io.tolgee.fixtures.waitFor
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.model.batch.BatchJob
import io.tolgee.model.batch.BatchJobStatus
import io.tolgee.security.JwtTokenProvider
import io.tolgee.testing.WebsocketTest
import io.tolgee.testing.assert
import io.tolgee.websocket.WebsocketTestHelper
import kotlinx.coroutines.ensureActive
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
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
  lateinit var translationChunkProcessor: TranslationChunkProcessor

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
  lateinit var jobChunkExecutionQueue: JobChunkExecutionQueue

  @BeforeEach
  fun setup() {
    jobChunkExecutionQueue.clear()
    Mockito.reset(translationChunkProcessor)
    Mockito.clearInvocations(translationChunkProcessor)
    whenever(translationChunkProcessor.getParams(any(), any())).thenCallRealMethod()
    whenever(translationChunkProcessor.getTarget(any())).thenCallRealMethod()
    whenever(deleteKeysChunkProcessor.getParams(any(), any())).thenCallRealMethod()
    whenever(deleteKeysChunkProcessor.getTarget(any())).thenCallRealMethod()
    jobChunkExecutionQueue.populateQueue()
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
    jobChunkExecutionQueue.clear()
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
        translationChunkProcessor,
        times(ceil(job.totalItems.toDouble() / BatchJobType.TRANSLATION.chunkSize).toInt())
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
      translationChunkProcessor.process(
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
    websocketHelper.receivedMessages.last.contains("FAILED")
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
      translationChunkProcessor.process(
        any(),
        argThat { this.containsAll((1L..10).toList()) },
        any(),
        any()
      )
    ).thenThrow(RuntimeException("OMG! It failed"))

    val job = runChunkedJob(1000)

    (1..3).forEach {
      waitForNotThrowing {
        jobChunkExecutionQueue.find { it.executeAfter == currentDateProvider.date.time + 2000 }.assert.isNotNull
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
    websocketHelper.receivedMessages.last.contains("FAILED")
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
      translationChunkProcessor.process(
        any(),
        argThat { this.containsAll(throwingChunk) },
        any(),
        any()
      )
    ).thenThrow(
      RequeueWithTimeoutException(
        message = Message.OUT_OF_CREDITS,
        successfulTargets = listOf(),
        cause = OutOfCreditsException(OutOfCreditsException.Reason.OUT_OF_CREDITS),
        timeoutInMs = 100,
        increaseFactor = 10,
        maxRetries = 3
      )
    )

    val job = runChunkedJob(1000)

    waitForNotThrowing {
      jobChunkExecutionQueue.find { it.executeAfter == currentDateProvider.date.time + 100 }.assert.isNotNull
    }

    currentDateProvider.forcedDate = Date(currentDateProvider.date.time + 100)

    waitForNotThrowing {
      jobChunkExecutionQueue.find { it.executeAfter == currentDateProvider.date.time + 1000 }.assert.isNotNull
    }

    currentDateProvider.forcedDate = Date(currentDateProvider.date.time + 1000)

    waitForNotThrowing {
      jobChunkExecutionQueue.find { it.executeAfter == currentDateProvider.date.time + 10000 }.assert.isNotNull
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
    websocketHelper.receivedMessages.last.contains("FAILED")
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

      // 100 progress messages + 1 finish message
      websocketHelper.receivedMessages.assert.hasSize(101)
      websocketHelper.receivedMessages.last.contains("SUCCESS")
    }
  }

  @Test
  fun `cancels the job`() {
    var count = 0

    whenever(translationChunkProcessor.process(any(), any(), any(), any())).then {
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
    websocketHelper.receivedMessages.last.contains("CANCELLED")
  }

  protected fun runChunkedJob(keyCount: Int): BatchJob {
    return executeInNewTransaction {
      batchJobService.startJob(
        request = BatchTranslateRequest().apply {
          keyIds = (1L..keyCount).map { it }
        },
        project = testData.projectBuilder.self,
        author = testData.user,
        type = BatchJobType.TRANSLATION
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
