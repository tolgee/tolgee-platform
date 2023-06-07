package io.tolgee.batch

import io.tolgee.AbstractSpringTest
import io.tolgee.development.testDataBuilder.data.BatchOperationsTestData
import io.tolgee.fixtures.waitForNotThrowing
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean

class BatchOperationsGeneralTest : AbstractSpringTest() {

  @Autowired
  lateinit var batchJobService: BatchJobService

  @Autowired
  @MockBean
  lateinit var chunkProcessingUtilFactory: ChunkProcessingUtil.Factory

  @Test
  fun `executes operation`() {
    val total = 1000
    val testData = BatchOperationsTestData()
    testDataService.saveTestData(testData.root)
    val job = batchJobService.startJob(
      BatchTranslateRequest().apply {
        keyIds = (1L..total).map { it }
      },
      testData.user,
      BatchJobType.TRANSLATION
    )

    val translationChunkProcessingUtilMock = mock<ChunkProcessingUtil>()
    whenever(chunkProcessingUtilFactory.invoke(any(), any())).thenAnswer { translationChunkProcessingUtilMock }

    waitForNotThrowing {
      verify(
        translationChunkProcessingUtilMock,
        times(total / BatchJobType.TRANSLATION.chunkSize + 1)
      ).processChunk()
    }
  }
}
