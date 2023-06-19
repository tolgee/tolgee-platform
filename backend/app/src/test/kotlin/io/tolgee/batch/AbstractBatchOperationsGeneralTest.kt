package io.tolgee.batch

import io.tolgee.AbstractSpringTest
import io.tolgee.constants.Message
import io.tolgee.development.testDataBuilder.data.BatchOperationsTestData
import io.tolgee.exceptions.OutOfCreditsException
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.model.batch.BatchJob
import io.tolgee.model.batch.BatchJobStatus
import io.tolgee.security.JwtTokenProvider
import io.tolgee.testing.assert
import io.tolgee.websocket.WebsocketTestHelper
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
import kotlin.math.ceil

abstract class AbstractBatchOperationsGeneralTest : AbstractSpringTest() {

  private lateinit var testData: BatchOperationsTestData

  @Autowired
  lateinit var batchJobService: BatchJobService

  @MockBean
  @Autowired
  lateinit var translationBatchProcessor: TranslationChunkProcessor

  @Autowired
  lateinit var batchJobActionService: BatchJobActionService

  @Autowired
  lateinit var jwtTokenProvider: JwtTokenProvider

  @LocalServerPort
  private val port: Int? = null

  lateinit var websocketHelper: WebsocketTestHelper

  @BeforeEach
  fun setup() {
    Mockito.reset(translationBatchProcessor)
    Mockito.clearInvocations(translationBatchProcessor)
    whenever(translationBatchProcessor.getParams(any(), any())).thenCallRealMethod()
    whenever(translationBatchProcessor.getTarget(any())).thenCallRealMethod()
    batchJobActionService.populateQueue()
    testData = BatchOperationsTestData()
    testDataService.saveTestData(testData.root)
  }

  @Test
  fun `executes operation`() {
    websocketHelper = WebsocketTestHelper(
      port,
      jwtTokenProvider.generateToken(testData.user.id).toString(),
      testData.projectBuilder.self.id
    )
    websocketHelper.listenForBatchOperationProgress()

    val job = runJob(1000)

    job.totalItems.assert.isEqualTo(1000)

    waitForNotThrowing(pollTime = 1000) {
      verify(
        translationBatchProcessor,
        times(ceil(job.totalItems.toDouble() / BatchJobType.TRANSLATION.chunkSize).toInt())
      ).process(any(), any())
    }

    waitForNotThrowing(pollTime = 1000) {
      val finishedJob = batchJobService.getJob(job.id)
      finishedJob.status.assert.isEqualTo(BatchJobStatus.SUCCESS)
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
    websocketHelper.listenForBatchOperationProgress()

    val job = runJob(1000)

    val exceptions = (1..50).map { _ ->
      FailedDontRequeueException(
        Message.OUT_OF_CREDITS,
        cause = OutOfCreditsException(OutOfCreditsException.Reason.OUT_OF_CREDITS),
        successfulTargets = listOf()
      )
    }

    whenever(translationBatchProcessor.process(any(), any()))
      .thenThrow(*exceptions.toTypedArray())
      .then {}

    waitForNotThrowing(pollTime = 1000) {
      val finishedJob = batchJobService.getJob(job.id)
      finishedJob.status.assert.isEqualTo(BatchJobStatus.FAILED)
    }

    // 100 progress messages + 1 finish message
    websocketHelper.receivedMessages.assert.hasSize(101)
    websocketHelper.receivedMessages.last.contains("FAILED")
  }

  @Test
  fun `retries failed with generic exception`() {
    websocketHelper = WebsocketTestHelper(
      port,
      jwtTokenProvider.generateToken(testData.user.id).toString(),
      testData.projectBuilder.self.id
    )
    websocketHelper.listenForBatchOperationProgress()

    whenever(
      translationBatchProcessor.process(
        any(),
        argThat { this.containsAll((1L..10).toList()) }
      )
    ).thenThrow(RuntimeException("OMG! It failed"))

    val job = runJob(1000)

    waitForNotThrowing(pollTime = 1000) {
      val finishedJob = batchJobService.getJob(job.id)
      finishedJob.status.assert.isEqualTo(BatchJobStatus.FAILED)
    }

    entityManager.createQuery("""from BatchJobChunkExecution b where b.batchJob.id = :id""")
      .setParameter("id", job.id).resultList.assert.hasSize(103)

    // 100 progress messages + 1 finish message
    websocketHelper.receivedMessages.assert.hasSize(100)
    websocketHelper.receivedMessages.last.contains("FAILED")
  }

  protected fun runJob(keyCount: Int): BatchJob {
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
}
