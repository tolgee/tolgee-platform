package io.tolgee.batch

import io.tolgee.AbstractSpringTest
import io.tolgee.configuration.BATCH_OPERATIONS_AFTER_WAIT_QUEUE
import io.tolgee.configuration.BATCH_OPERATIONS_FAILED_CHUNKS_QUEUE
import io.tolgee.configuration.BATCH_OPERATIONS_QUEUE
import io.tolgee.configuration.BATCH_OPERATIONS_WAIT_QUEUE
import io.tolgee.development.testDataBuilder.data.BatchOperationsTestData
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.amqp.core.AmqpAdmin
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import kotlin.math.ceil

@SpringBootTest
class BatchOperationsGeneralTest : AbstractSpringTest() {

  private lateinit var translationChunkProcessingUtilMock: ChunkProcessingUtil

  @Autowired
  lateinit var batchJobService: BatchJobService

  @Autowired
  @MockBean
  lateinit var chunkProcessingUtilFactory: ChunkProcessingUtilFactory

  @Autowired
  lateinit var amqpAdmin: AmqpAdmin

  @BeforeEach
  fun setup() {
    amqpAdmin.purgeQueue(BATCH_OPERATIONS_QUEUE)
    amqpAdmin.purgeQueue(BATCH_OPERATIONS_AFTER_WAIT_QUEUE)
    amqpAdmin.purgeQueue(BATCH_OPERATIONS_WAIT_QUEUE)
    amqpAdmin.purgeQueue(BATCH_OPERATIONS_FAILED_CHUNKS_QUEUE)
    translationChunkProcessingUtilMock = mock<ChunkProcessingUtil>()
    whenever(chunkProcessingUtilFactory.process(any(), any())).thenReturn(translationChunkProcessingUtilMock)
    assertThat(chunkProcessingUtilFactory.process(mock(), mock())).isEqualTo(translationChunkProcessingUtilMock)
  }

  @Test
  fun `executes operation`() {
    val total = 1000
    val testData = BatchOperationsTestData()
    testDataService.saveTestData(testData.root)

    batchJobService.startJob(
      BatchTranslateRequest().apply {
        keyIds = (1L..total).map { it }
      },
      testData.user,
      BatchJobType.TRANSLATION
    )

    waitForNotThrowing {
      verify(
        translationChunkProcessingUtilMock,
        times(ceil(total.toDouble() / BatchJobType.TRANSLATION.chunkSize).toInt())
      ).processChunk()
    }
  }
}
