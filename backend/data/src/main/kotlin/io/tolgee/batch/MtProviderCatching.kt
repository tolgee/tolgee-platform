package io.tolgee.batch

import io.tolgee.batch.data.BatchTranslationTargetItem
import io.tolgee.component.CurrentDateProvider
import io.tolgee.constants.Message
import io.tolgee.exceptions.FormalityNotSupportedException
import io.tolgee.exceptions.LanguageNotSupportedException
import io.tolgee.exceptions.LlmContentFilterException
import io.tolgee.exceptions.LlmEmptyResponseException
import io.tolgee.exceptions.LlmProviderMaxTokensExceededException
import io.tolgee.exceptions.LlmProviderNotReturnedJsonException
import io.tolgee.exceptions.LlmRateLimitedException
import io.tolgee.exceptions.OutOfCreditsException
import io.tolgee.exceptions.limits.PlanLimitExceededStringsException
import io.tolgee.exceptions.limits.PlanSpendingLimitExceededStringsException
import jakarta.persistence.EntityNotFoundException
import kotlinx.coroutines.ensureActive
import org.springframework.stereotype.Component
import kotlin.coroutines.CoroutineContext

@Component
class MtProviderCatching(
  private val currentDateProvider: CurrentDateProvider,
) {
  fun iterateCatching(
    chunk: List<BatchTranslationTargetItem>,
    coroutineContext: CoroutineContext,
    fn: (item: BatchTranslationTargetItem) -> Unit,
  ) {
    val exceptions = mutableListOf<ChunkItemFailedException>()
    val successfulTargets = mutableListOf<BatchTranslationTargetItem>()
    chunk.forEach { item ->
      coroutineContext.ensureActive()
      try {
        fn(item)
        successfulTargets.add(item)
      } catch (e: OutOfCreditsException) {
        throw FailedDontRequeueException(Message.OUT_OF_CREDITS, successfulTargets, e)
      } catch (e: LlmContentFilterException) {
        throw FailedDontRequeueException(Message.LLM_CONTENT_FILTER, successfulTargets, e)
      } catch (e: LlmEmptyResponseException) {
        // maxRetries = 0: malformed responses are consistent for the same input, retrying doesn't
        // help — but the remaining items of the chunk must still be processed
        exceptions.add(
          ChunkItemFailedException(Message.LLM_PROVIDER_EMPTY_RESPONSE, successfulTargets, e, maxRetries = 0),
        )
      } catch (e: LlmProviderNotReturnedJsonException) {
        exceptions.add(
          ChunkItemFailedException(Message.LLM_PROVIDER_NOT_RETURNED_JSON, successfulTargets, e, maxRetries = 0),
        )
      } catch (e: LlmProviderMaxTokensExceededException) {
        exceptions.add(
          ChunkItemFailedException(Message.LLM_PROVIDER_MAX_TOKENS_EXCEEDED, successfulTargets, e, maxRetries = 0),
        )
      } catch (e: LlmRateLimitedException) {
        exceptions.add(
          ChunkItemFailedException(
            Message.LLM_RATE_LIMITED,
            successfulTargets,
            e,
            e.retryAt?.let { (e.retryAt - currentDateProvider.date.time).toInt() } ?: 100,
            increaseFactor = 1,
            maxRetries = -1,
          ),
        )
      } catch (e: PlanLimitExceededStringsException) {
        throw FailedDontRequeueException(Message.PLAN_TRANSLATION_LIMIT_EXCEEDED, successfulTargets, e)
      } catch (e: PlanSpendingLimitExceededStringsException) {
        throw FailedDontRequeueException(Message.TRANSLATION_SPENDING_LIMIT_EXCEEDED, successfulTargets, e)
      } catch (e: FormalityNotSupportedException) {
        throw FailedDontRequeueException(e.tolgeeMessage!!, successfulTargets, e)
      } catch (e: LanguageNotSupportedException) {
        throw FailedDontRequeueException(e.tolgeeMessage!!, successfulTargets, e)
      } catch (e: EntityNotFoundException) {
        throw FailedDontRequeueException(Message.TRANSLATION_FAILED, successfulTargets, e)
      } catch (e: Throwable) {
        exceptions.add(ChunkItemFailedException(Message.TRANSLATION_FAILED, successfulTargets, e))
      }
    }
    if (exceptions.isNotEmpty()) {
      if (exceptions.size == 1) {
        // remap with using successfulTargets declared above
        val exception = exceptions.first()
        throw ChunkItemFailedException(
          message = exception.tolgeeMessage ?: Message.TRANSLATION_FAILED,
          successfulTargets = successfulTargets,
          cause = exception.cause,
          delayInMs = exception.delayInMs,
          increaseFactor = exception.increaseFactor,
          maxRetries = exception.maxRetries,
        )
      }
      throw MultipleItemsFailedException(
        exceptions = exceptions.toList(),
        successfulTargets = successfulTargets,
      )
    }
  }
}
