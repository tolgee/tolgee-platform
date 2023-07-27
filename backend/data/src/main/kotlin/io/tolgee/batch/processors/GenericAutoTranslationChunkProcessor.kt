package io.tolgee.batch.processors

import io.tolgee.batch.BatchJobDto
import io.tolgee.batch.FailedDontRequeueException
import io.tolgee.batch.RequeueWithDelayException
import io.tolgee.constants.Message
import io.tolgee.exceptions.OutOfCreditsException
import io.tolgee.model.Language
import io.tolgee.service.key.KeyService
import io.tolgee.service.translation.AutoTranslationService
import kotlinx.coroutines.ensureActive
import org.springframework.stereotype.Component
import kotlin.coroutines.CoroutineContext

@Component
class GenericAutoTranslationChunkProcessor(
  private val autoTranslationService: AutoTranslationService,
  private val keyService: KeyService,
) {
  fun process(
    job: BatchJobDto,
    chunk: List<Long>,
    coroutineContext: CoroutineContext,
    onProgress: (Int) -> Unit,
    type: Type,
    languages: List<Language>
  ) {
    val keys = keyService.find(chunk)
    val successfulTargets = mutableListOf<Long>()
    keys.forEach { key ->
      coroutineContext.ensureActive()
      try {
        autoTranslationService.autoTranslate(
          key = key,
          languageTags = languages.map { it.tag },
          useTranslationMemory = type == Type.PRE_TRANSLATION_BY_TM,
          useMachineTranslation = type == Type.MACHINE_TRANSLATION,
        )
        successfulTargets.add(key.id)
      } catch (e: OutOfCreditsException) {
        throw FailedDontRequeueException(Message.OUT_OF_CREDITS, successfulTargets, e)
      } catch (e: Throwable) {
        throw RequeueWithDelayException(Message.TRANSLATION_FAILED, successfulTargets, e)
      }
    }
  }

  enum class Type {
    MACHINE_TRANSLATION, PRE_TRANSLATION_BY_TM
  }
}
