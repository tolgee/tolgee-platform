package io.tolgee.batch.processors

import io.tolgee.batch.MtProviderCatching
import io.tolgee.batch.data.BatchJobDto
import io.tolgee.batch.data.BatchTranslationTargetItem
import io.tolgee.service.PromptService
import io.tolgee.service.key.KeyService
import io.tolgee.service.language.LanguageService
import io.tolgee.service.translation.AutoTranslationService
import org.springframework.stereotype.Component
import kotlin.coroutines.CoroutineContext

@Component
class GenericAutoTranslationChunkProcessor(
  private val autoTranslationService: AutoTranslationService,
  private val keyService: KeyService,
  private val languageService: LanguageService,
  private val promptService: PromptService,
  private val mtProviderCatching: MtProviderCatching,
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

    mtProviderCatching.iterateCatching(chunk, coroutineContext) { item ->
      val (keyId, languageId) = item
      val languageTag = languages[languageId]?.tag ?: return@iterateCatching
      val key = keys[keyId] ?: return@iterateCatching
      autoTranslationService.autoTranslateSync(
        key = key,
        forcedLanguageTags = listOf(languageTag),
        useTranslationMemory = useTranslationMemory,
        useMachineTranslation = useMachineTranslation,
        isBatch = true,
      )
    }
  }
}
