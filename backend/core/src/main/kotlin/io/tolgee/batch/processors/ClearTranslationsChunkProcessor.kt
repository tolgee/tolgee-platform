package io.tolgee.batch.processors

import io.tolgee.batch.ChunkProcessor
import io.tolgee.batch.ProgressManager
import io.tolgee.batch.data.BatchJobDto
import io.tolgee.batch.request.ClearTranslationsRequest
import io.tolgee.model.batch.params.ClearTranslationsJobParams
import io.tolgee.service.translation.TranslationService
import jakarta.persistence.EntityManager
import kotlinx.coroutines.ensureActive
import org.springframework.stereotype.Component
import kotlin.coroutines.CoroutineContext

@Component
class ClearTranslationsChunkProcessor(
  private val translationService: TranslationService,
  private val entityManager: EntityManager,
  private val progressManager: ProgressManager,
) : ChunkProcessor<ClearTranslationsRequest, ClearTranslationsJobParams, Long> {
  override fun process(
    job: BatchJobDto,
    chunk: List<Long>,
    coroutineContext: CoroutineContext,
  ) {
    val subChunked = chunk.chunked(100)
    val params = getParams(job)
    subChunked.forEach { subChunk ->
      coroutineContext.ensureActive()
      @Suppress("UNCHECKED_CAST")
      translationService.clearBatch(subChunk, params.languageIds)
      entityManager.flush()
      progressManager.reportSingleChunkProgress(job.id, subChunk.size)
    }
  }

  override fun getParamsType(): Class<ClearTranslationsJobParams> {
    return ClearTranslationsJobParams::class.java
  }

  override fun getTargetItemType(): Class<Long> {
    return Long::class.java
  }

  override fun getTarget(data: ClearTranslationsRequest): List<Long> {
    return data.keyIds
  }

  override fun getParams(data: ClearTranslationsRequest): ClearTranslationsJobParams {
    return ClearTranslationsJobParams().apply {
      languageIds = data.languageIds
    }
  }
}
