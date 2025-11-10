package io.tolgee.batch

import io.tolgee.batch.data.BatchTranslationTargetItem
import io.tolgee.component.CurrentDateProvider
import io.tolgee.constants.Message
import io.tolgee.exceptions.FormalityNotSupportedException
import io.tolgee.exceptions.LanguageNotSupportedException
import io.tolgee.exceptions.LlmContentFilterException
import io.tolgee.exceptions.LlmEmptyResponseException
import io.tolgee.exceptions.LlmProviderNotReturnedJsonException
import io.tolgee.exceptions.LlmRateLimitedException
import io.tolgee.exceptions.OutOfCreditsException
import io.tolgee.exceptions.limits.PlanLimitExceededStringsException
import io.tolgee.exceptions.limits.PlanSpendingLimitExceededStringsException
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
        throw FailedDontRequeueException(Message.LLM_PROVIDER_EMPTY_RESPONSE, successfulTargets, e)
      } catch (e: LlmProviderNotReturnedJsonException) {
        throw FailedDontRequeueException(Message.LLM_PROVIDER_NOT_RETURNED_JSON, successfulTargets, e)
      } catch (e: LlmRateLimitedException) {
        throw RequeueWithDelayException(
          Message.LLM_RATE_LIMITED,
          successfulTargets,
          e,
          e.retryAt?.let { (e.retryAt - currentDateProvider.date.time).toInt() } ?: 100,
          increaseFactor = 1,
          maxRetries = -1,
        )
      } catch (e: PlanLimitExceededStringsException) {
        throw FailedDontRequeueException(Message.PLAN_TRANSLATION_LIMIT_EXCEEDED, successfulTargets, e)
      } catch (e: PlanSpendingLimitExceededStringsException) {
        throw FailedDontRequeueException(Message.TRANSLATION_SPENDING_LIMIT_EXCEEDED, successfulTargets, e)
      } catch (e: FormalityNotSupportedException) {
        throw FailedDontRequeueException(e.tolgeeMessage!!, successfulTargets, e)
      } catch (e: LanguageNotSupportedException) {
        throw FailedDontRequeueException(e.tolgeeMessage!!, successfulTargets, e)
      } catch (e: Throwable) {
        throw RequeueWithDelayException(Message.TRANSLATION_FAILED, successfulTargets, e)
      }
    }
  }
}
