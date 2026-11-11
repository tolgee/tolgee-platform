package io.tolgee.batch

import io.tolgee.batch.data.BatchJobDto
import io.tolgee.batch.data.BatchTranslationTargetItem
import io.tolgee.development.testDataBuilder.data.BatchJobTargetTestData
import io.tolgee.testing.AbstractControllerTest
import io.tolgee.testing.assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class BatchJobTargetProviderTest : AbstractControllerTest() {
  @Autowired
  private lateinit var batchJobTargetProvider: BatchJobTargetProvider

  private lateinit var testData: BatchJobTargetTestData

  @BeforeEach
  fun setup() {
    testData = BatchJobTargetTestData()
    testDataService.saveTestData(testData.root)
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `loads a product target from the database`() {
    batchJobTargetProvider.evict(testData.productJob.id)

    batchJobTargetProvider
      .getChunk(BatchJobDto.fromEntity(testData.productJob), 1)
      .assert
      .containsExactly(
        BatchTranslationTargetItem(1L, 30L),
        BatchTranslationTargetItem(2L, 10L),
      )
  }

  @Test
  fun `loads a list target from the database`() {
    batchJobTargetProvider.evict(testData.listJob.id)

    batchJobTargetProvider
      .getChunk(BatchJobDto.fromEntity(testData.listJob), 1)
      .map { (it as Number).toLong() }
      .assert
      .containsExactly(9L)
  }
}
