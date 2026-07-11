package io.tolgee.batch.processors

import io.tolgee.batch.JobCharacter
import io.tolgee.batch.data.BatchTranslationTargetItem
import io.tolgee.batch.request.QaCheckRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock

class QaCheckChunkProcessorTest {
  private val processor =
    QaCheckChunkProcessor(
      qaCheckBatchService = mock(),
      progressManager = mock(),
      objectMapper = mock(),
    )

  private fun request(targetSize: Int): QaCheckRequest =
    QaCheckRequest(
      target = (1..targetSize).map { BatchTranslationTargetItem(keyId = it.toLong(), languageId = 1L) },
    )

  @Test
  fun `single item is FAST`() {
    assertThat(processor.getJobCharacter(request(1), projectId = 1L)).isEqualTo(JobCharacter.FAST)
  }

  @Test
  fun `below threshold is FAST`() {
    assertThat(processor.getJobCharacter(request(9), projectId = 1L)).isEqualTo(JobCharacter.FAST)
  }

  @Test
  fun `large target is SLOW`() {
    assertThat(processor.getJobCharacter(request(50), projectId = 1L)).isEqualTo(JobCharacter.SLOW)
  }
}
