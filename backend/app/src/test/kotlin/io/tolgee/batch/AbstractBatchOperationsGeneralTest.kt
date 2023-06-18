package io.tolgee.batch

import io.tolgee.AbstractSpringTest
import io.tolgee.development.testDataBuilder.data.BatchOperationsTestData
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
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.web.server.LocalServerPort
import kotlin.math.ceil

@SpringBootTest
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
    websocketHelper.listen()

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

    websocketHelper.receivedMessages.assert.hasSize(100)
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
