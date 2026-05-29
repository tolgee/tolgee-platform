package io.tolgee.batch.processors

import com.fasterxml.jackson.databind.ObjectMapper
import io.tolgee.batch.AbstractChunkProcessor
import io.tolgee.batch.JobCharacter
import io.tolgee.batch.MtProviderCatching
import io.tolgee.batch.ProgressManager
import io.tolgee.batch.data.BatchJobDto
import io.tolgee.batch.data.BatchTranslationTargetItem
import io.tolgee.batch.request.AutoTranslationRequest
import io.tolgee.configuration.tolgee.BatchProperties
import io.tolgee.model.batch.params.AutoTranslationJobParams
import io.tolgee.service.key.KeyService
import io.tolgee.service.language.LanguageService
import io.tolgee.service.translation.AutoTranslationService
import io.tolgee.service.translationMemory.TmAutoTranslateProvider
import org.springframework.stereotype.Component
import kotlin.coroutines.CoroutineContext

@Component
class AutoTranslateChunkProcessor(
  private val autoTranslationService: AutoTranslationService,
  private val mtProviderCatching: MtProviderCatching,
  private val batchProperties: BatchProperties,
  private val progressManager: ProgressManager,
  private val tmAutoTranslateProvider: TmAutoTranslateProvider,
  private val keyService: KeyService,
  private val languageService: LanguageService,
  objectMapper: ObjectMapper,
) : AbstractChunkProcessor<AutoTranslationRequest, AutoTranslationJobParams, BatchTranslationTargetItem>(objectMapper) {
  override fun process(
    job: BatchJobDto,
    chunk: List<BatchTranslationTargetItem>,
    coroutineContext: CoroutineContext,
  ) {
    val projectId = job.projectId ?: throw IllegalArgumentException("Project id is required")
    val keys = keyService.find(chunk.map { it.keyId }).associateBy { it.id }
    val languages = languageService.findByIdIn(chunk.map { it.languageId }.toSet()).associateBy { it.id }
    val precomputedTmMatches =
      tmAutoTranslateProvider.getAutoTranslatedValuesForChunk(
        items =
          chunk
            .filter { keys.containsKey(it.keyId) && languages.containsKey(it.languageId) }
            .map { it.keyId to it.languageId },
        keysById = keys,
        languagesById = languages,
      )
    mtProviderCatching.iterateCatching(chunk, coroutineContext) { item ->
      val (keyId, languageId) = item
      autoTranslationService.softAutoTranslate(projectId, keyId, languageId, precomputedTmMatches)
      progressManager.reportSingleChunkProgress(job.id)
    }
  }

  override fun getParamsType(): Class<AutoTranslationJobParams> {
    return AutoTranslationJobParams::class.java
  }

  override fun getTarget(data: AutoTranslationRequest): List<BatchTranslationTargetItem> {
    return data.target
  }

  override fun getMaxPerJobConcurrency(): Int {
    return batchProperties.maxPerMtJobConcurrency
  }

  override fun getJobCharacter(
    request: AutoTranslationRequest,
    projectId: Long?,
  ): JobCharacter {
    return JobCharacter.SLOW
  }

  override fun getChunkSize(
    request: AutoTranslationRequest,
    projectId: Long?,
  ): Int {
    return 3
  }

  override fun getTargetItemType(): Class<BatchTranslationTargetItem> {
    return BatchTranslationTargetItem::class.java
  }

  override fun getParams(data: AutoTranslationRequest): AutoTranslationJobParams {
    return AutoTranslationJobParams()
  }
}
