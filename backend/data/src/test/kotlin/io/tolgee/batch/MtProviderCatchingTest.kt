package io.tolgee.batch

import io.tolgee.batch.data.BatchTranslationTargetItem
import io.tolgee.constants.Message
import io.tolgee.exceptions.LlmEmptyResponseException
import io.tolgee.exceptions.LlmProviderMaxTokensExceededException
import io.tolgee.exceptions.LlmProviderNotReturnedJsonException
import io.tolgee.exceptions.OutOfCreditsException
import io.tolgee.testing.assert
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import kotlin.coroutines.EmptyCoroutineContext

class MtProviderCatchingTest {
  private val mtProviderCatching = MtProviderCatching(currentDateProvider = mock())

  private val chunk = (1L..3L).map { BatchTranslationTargetItem(keyId = it, languageId = 1L) }

  @Test
  fun `continues with other items when llm does not return json and does not retry the failed one`() {
    val processed = mutableListOf<BatchTranslationTargetItem>()

    val thrown =
      catchThrowable {
        mtProviderCatching.iterateCatching(chunk, EmptyCoroutineContext) { item ->
          processed.add(item)
          if (item.keyId == 2L) throw LlmProviderNotReturnedJsonException()
        }
      }

    processed.assert.isEqualTo(chunk)
    thrown.assert.isInstanceOf(ChunkItemFailedException::class.java)
    thrown as ChunkItemFailedException
    thrown.tolgeeMessage.assert.isEqualTo(Message.LLM_PROVIDER_NOT_RETURNED_JSON)
    thrown.successfulTargets.assert.containsExactly(chunk[0], chunk[2])
    thrown.maxRetries.assert.isEqualTo(0)
  }

  @Test
  fun `continues with other items when llm output is truncated and does not retry the failed one`() {
    val processed = mutableListOf<BatchTranslationTargetItem>()

    val thrown =
      catchThrowable {
        mtProviderCatching.iterateCatching(chunk, EmptyCoroutineContext) { item ->
          processed.add(item)
          if (item.keyId == 2L) throw LlmProviderMaxTokensExceededException()
        }
      }

    processed.assert.isEqualTo(chunk)
    thrown.assert.isInstanceOf(ChunkItemFailedException::class.java)
    thrown as ChunkItemFailedException
    thrown.tolgeeMessage.assert.isEqualTo(Message.LLM_PROVIDER_MAX_TOKENS_EXCEEDED)
    thrown.successfulTargets.assert.containsExactly(chunk[0], chunk[2])
    thrown.maxRetries.assert.isEqualTo(0)
  }

  @Test
  fun `continues with other items when llm returns empty response and does not retry the failed one`() {
    val processed = mutableListOf<BatchTranslationTargetItem>()

    val thrown =
      catchThrowable {
        mtProviderCatching.iterateCatching(chunk, EmptyCoroutineContext) { item ->
          processed.add(item)
          if (item.keyId == 2L) throw LlmEmptyResponseException()
        }
      }

    processed.assert.isEqualTo(chunk)
    thrown.assert.isInstanceOf(ChunkItemFailedException::class.java)
    thrown as ChunkItemFailedException
    thrown.tolgeeMessage.assert.isEqualTo(Message.LLM_PROVIDER_EMPTY_RESPONSE)
    thrown.successfulTargets.assert.containsExactly(chunk[0], chunk[2])
    thrown.maxRetries.assert.isEqualTo(0)
  }

  @Test
  fun `aggregates multiple llm response failures and processes all items`() {
    val processed = mutableListOf<BatchTranslationTargetItem>()

    val thrown =
      catchThrowable {
        mtProviderCatching.iterateCatching(chunk, EmptyCoroutineContext) { item ->
          processed.add(item)
          if (item.keyId == 1L) throw LlmProviderNotReturnedJsonException()
          if (item.keyId == 2L) throw LlmEmptyResponseException()
        }
      }

    processed.assert.isEqualTo(chunk)
    thrown.assert.isInstanceOf(MultipleItemsFailedException::class.java)
    thrown as MultipleItemsFailedException
    thrown.exceptions.assert.hasSize(2)
    thrown.successfulTargets.assert.containsExactly(chunk[2])
  }

  @Test
  fun `stops iteration and does not requeue when out of credits`() {
    val processed = mutableListOf<BatchTranslationTargetItem>()

    val thrown =
      catchThrowable {
        mtProviderCatching.iterateCatching(chunk, EmptyCoroutineContext) { item ->
          processed.add(item)
          if (item.keyId == 2L) throw OutOfCreditsException(OutOfCreditsException.Reason.OUT_OF_CREDITS)
        }
      }

    processed.assert.isEqualTo(chunk.take(2))
    thrown.assert.isInstanceOf(FailedDontRequeueException::class.java)
    thrown as FailedDontRequeueException
    thrown.successfulTargets.assert.containsExactly(chunk[0])
  }
}
