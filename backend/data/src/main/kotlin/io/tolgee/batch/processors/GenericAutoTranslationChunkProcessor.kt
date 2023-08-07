package io.tolgee.batch.processors

import io.tolgee.batch.FailedDontRequeueException
import io.tolgee.batch.RequeueWithDelayException
import io.tolgee.batch.data.BatchJobDto
import io.tolgee.batch.data.BatchTranslationTargetItem
import io.tolgee.component.CurrentDateProvider
import io.tolgee.component.machineTranslation.TranslationApiRateLimitException
import io.tolgee.constants.Message
import io.tolgee.exceptions.OutOfCreditsException
import io.tolgee.service.LanguageService
import io.tolgee.service.key.KeyService
import io.tolgee.service.translation.AutoTranslationService
import kotlinx.coroutines.ensureActive
import org.springframework.stereotype.Component
import kotlin.coroutines.CoroutineContext

@Component
class GenericAutoTranslationChunkProcessor(
  private val autoTranslationService: AutoTranslationService,
  private val keyService: KeyService,
  private val currentDateProvider: CurrentDateProvider,
  private val languageService: LanguageService
) {
  fun process(
    job: BatchJobDto,
    chunk: List<BatchTranslationTargetItem>,
    coroutineContext: CoroutineContext,
    onProgress: (Int) -> Unit,
    useTranslationMemory: Boolean,
    useMachineTranslation: Boolean,
  ) {
    val languages = languageService.findByIdIn(chunk.map { it.languageId }.toSet()).associateBy { it.id }
    val keys = keyService.find(chunk.map { it.keyId }).associateBy { it.id }
    chunk.forEach { item ->
      val (keyId, languageId) = item
      coroutineContext.ensureActive()
      doCatching(item) {
        val languageTag = languages[languageId]?.tag ?: return@doCatching
        val key = keys[keyId] ?: return@doCatching
        autoTranslationService.autoTranslateSync(
          key = key,
          languageTags = listOf(languageTag),
          useTranslationMemory = useTranslationMemory,
          useMachineTranslation = useMachineTranslation,
          isBatch = true
        )
      }
    }
  }

  fun doCatching(item: BatchTranslationTargetItem, fn: () -> Unit) {
    val successfulTargets = mutableListOf<BatchTranslationTargetItem>()
    try {
      fn()
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
        maxRetries = -1
      )
    } catch (e: Throwable) {
      throw RequeueWithDelayException(Message.TRANSLATION_FAILED, successfulTargets, e)
    }
  }
}
