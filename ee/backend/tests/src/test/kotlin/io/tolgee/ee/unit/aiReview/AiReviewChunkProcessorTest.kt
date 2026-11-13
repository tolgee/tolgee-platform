package io.tolgee.ee.unit.aiReview

import io.tolgee.batch.ChunkItemFailedException
import io.tolgee.batch.ProgressManager
import io.tolgee.batch.data.BatchJobDto
import io.tolgee.batch.processors.AiReviewChunkProcessor
import io.tolgee.component.CurrentDateProvider
import io.tolgee.constants.Message
import io.tolgee.exceptions.LlmRateLimitedException
import io.tolgee.service.aiReview.AiReviewBatchService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowableOfType
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.util.Date
import kotlin.coroutines.EmptyCoroutineContext

class AiReviewChunkProcessorTest {
  @Test
  fun `a rate limit retries the chunk after retryAt without a retry limit and keeps the done items`() {
    val service = mock(AiReviewBatchService::class.java)
    val dateProvider = mock(CurrentDateProvider::class.java)
    whenever(dateProvider.date).thenReturn(Date(1_000))
    whenever(service.reviewTranslation(any(), eq(2L))).thenThrow(LlmRateLimitedException(retryAt = 61_000))
    val processor =
      AiReviewChunkProcessor(service, mock(ProgressManager::class.java), dateProvider, jacksonObjectMapper())

    val exception =
      catchThrowableOfType(ChunkItemFailedException::class.java) {
        processor.process(mock(BatchJobDto::class.java), listOf(1L, 2L, 3L), EmptyCoroutineContext)
      }

    assertThat(exception.tolgeeMessage).isEqualTo(Message.LLM_RATE_LIMITED)
    assertThat(exception.delayInMs).isEqualTo(60_000)
    assertThat(exception.increaseFactor).isEqualTo(1)
    assertThat(exception.maxRetries).isEqualTo(-1)
    assertThat(exception.successfulTargets).containsExactly(1L)
  }
}
