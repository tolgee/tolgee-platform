package io.tolgee.batch

import io.tolgee.AbstractSpringTest
import io.tolgee.development.testDataBuilder.data.BatchOperationsTestData
import io.tolgee.fixtures.waitForNotThrowing
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
import kotlin.math.ceil

@SpringBootTest
class BatchOperationsGeneralTest : AbstractSpringTest() {

  @Autowired
  lateinit var batchJobService: BatchJobService

  @MockBean
  @Autowired
  lateinit var translationBatchProcessor: TranslationChunkProcessor

  @Autowired
  lateinit var batchJobActionService: BatchJobActionService

  @BeforeEach
  fun setup() {
    Mockito.clearInvocations(translationBatchProcessor)
    whenever(translationBatchProcessor.getParams(any(), any())).thenCallRealMethod()
    whenever(translationBatchProcessor.getTarget(any())).thenCallRealMethod()
    whenever(translationBatchProcessor.process(any(), any())).thenAnswer {
//      sleep((0..200).random().toLong())
    }
    batchJobActionService.populateQueue()
  }

  @Test
  fun `executes operation`() {
    val total = 1000
    val testData = BatchOperationsTestData()
    testDataService.saveTestData(testData.root)

    executeInNewTransaction {
      batchJobService.startJob(
        BatchTranslateRequest().apply {
          keyIds = (1L..total).map { it }
        },
        testData.user,
        BatchJobType.TRANSLATION
      )
    }

    waitForNotThrowing {
      verify(
        translationBatchProcessor,
        times(ceil(total.toDouble() / BatchJobType.TRANSLATION.chunkSize).toInt())
      ).process(any(), any())
    }
  }
}
