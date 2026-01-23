package io.tolgee.batch.processors

import io.tolgee.batch.ChunkProcessor
import io.tolgee.batch.JobCharacter
import io.tolgee.batch.MtProviderCatching
import io.tolgee.batch.ProgressManager
import io.tolgee.batch.data.BatchJobDto
import io.tolgee.batch.data.BatchTranslationTargetItem
import io.tolgee.batch.request.AutoTranslationRequest
import io.tolgee.configuration.tolgee.BatchProperties
import io.tolgee.model.batch.params.AutoTranslationJobParams
import io.tolgee.service.translation.AutoTranslationService
import org.springframework.stereotype.Component
import kotlin.coroutines.CoroutineContext

@Component
class AutoTranslateChunkProcessor(
  private val autoTranslationService: AutoTranslationService,
  private val mtProviderCatching: MtProviderCatching,
  private val batchProperties: BatchProperties,
  private val progressManager: ProgressManager,
) : ChunkProcessor<AutoTranslationRequest, AutoTranslationJobParams, BatchTranslationTargetItem> {
  override fun process(
    job: BatchJobDto,
    chunk: List<BatchTranslationTargetItem>,
    coroutineContext: CoroutineContext,
  ) {
    val projectId = job.projectId ?: throw IllegalArgumentException("Project id is required")
    mtProviderCatching.iterateCatching(chunk, coroutineContext) { item ->
      val (keyId, languageId) = item
      autoTranslationService.softAutoTranslate(projectId, keyId, languageId)
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

  override fun getJobCharacter(): JobCharacter {
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
