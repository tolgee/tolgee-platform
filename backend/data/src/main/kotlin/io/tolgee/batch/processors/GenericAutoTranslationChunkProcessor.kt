package io.tolgee.batch.processors

import io.tolgee.batch.FailedDontRequeueException
import io.tolgee.batch.RequeueWithDelayException
import io.tolgee.batch.data.BatchJobDto
import io.tolgee.batch.data.BatchTranslationTargetItem
import io.tolgee.component.CurrentDateProvider
import io.tolgee.component.machineTranslation.TranslationApiRateLimitException
import io.tolgee.constants.Message
import io.tolgee.dtos.request.prompt.PromptDto
import io.tolgee.dtos.request.prompt.PromptRunDto
import io.tolgee.exceptions.FormalityNotSupportedException
import io.tolgee.exceptions.LanguageNotSupportedException
import io.tolgee.exceptions.OutOfCreditsException
import io.tolgee.exceptions.PlanTranslationLimitExceeded
import io.tolgee.exceptions.TranslationSpendingLimitExceeded
import io.tolgee.model.enums.LLMProviderPriority
import io.tolgee.service.PromptService
import io.tolgee.service.key.KeyService
import io.tolgee.service.language.LanguageService
import io.tolgee.service.translation.AutoTranslationService
import kotlinx.coroutines.ensureActive
import org.springframework.stereotype.Component
import kotlin.coroutines.CoroutineContext

@Component
class GenericAutoTranslationChunkProcessor(
  private val autoTranslationService: AutoTranslationService,
  private val keyService: KeyService,
  private val currentDateProvider: CurrentDateProvider,
  private val languageService: LanguageService,
  private val promptService: PromptService,
) {
  fun process(
    job: BatchJobDto,
    chunk: List<BatchTranslationTargetItem>,
    coroutineContext: CoroutineContext,
    onProgress: (Int) -> Unit,
    useTranslationMemory: Boolean,
    useMachineTranslation: Boolean,
    llmPrompt: PromptDto? = null,
  ) {
    val languages = languageService.findByIdIn(chunk.map { it.languageId }.toSet()).associateBy { it.id }
    val keys = keyService.find(chunk.map { it.keyId }).associateBy { it.id }

    iterateCatching(chunk, coroutineContext) { item ->
      val (keyId, languageId) = item
      val languageTag = languages[languageId]?.tag ?: return@iterateCatching
      val key = keys[keyId] ?: return@iterateCatching
      if (llmPrompt == null) {
        autoTranslationService.autoTranslateSync(
          key = key,
          forcedLanguageTags = listOf(languageTag),
          useTranslationMemory = useTranslationMemory,
          useMachineTranslation = useMachineTranslation,
          isBatch = true,
        )
      } else {
        promptService.translateAndUpdateTranslation(
          job.projectId!!,
          PromptRunDto(
            template = llmPrompt.template,
            keyId = key.id,
            targetLanguageId = languageId,
            provider = llmPrompt.providerName,
          ),
          priority = LLMProviderPriority.LOW,
        )
      }
    }
  }

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
      } catch (e: TranslationApiRateLimitException) {
        throw RequeueWithDelayException(
          Message.TRANSLATION_API_RATE_LIMIT,
          successfulTargets,
          e,
          (e.retryAt - currentDateProvider.date.time).toInt(),
          increaseFactor = 1,
          maxRetries = -1,
        )
      } catch (e: PlanTranslationLimitExceeded) {
        throw FailedDontRequeueException(Message.PLAN_TRANSLATION_LIMIT_EXCEEDED, successfulTargets, e)
      } catch (e: TranslationSpendingLimitExceeded) {
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
