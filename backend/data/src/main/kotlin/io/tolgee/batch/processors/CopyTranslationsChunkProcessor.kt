package io.tolgee.batch.processors

import io.tolgee.batch.ChunkProcessor
import io.tolgee.batch.data.BatchJobDto
import io.tolgee.batch.request.CopyTranslationRequest
import io.tolgee.model.batch.params.CopyTranslationJobParams
import io.tolgee.service.translation.TranslationService
import jakarta.persistence.EntityManager
import kotlinx.coroutines.ensureActive
import org.springframework.stereotype.Component
import kotlin.coroutines.CoroutineContext

@Component
class CopyTranslationsChunkProcessor(
  private val translationService: TranslationService,
  private val entityManager: EntityManager,
) : ChunkProcessor<CopyTranslationRequest, CopyTranslationJobParams, Long> {
  override fun process(
    job: BatchJobDto,
    chunk: List<Long>,
    coroutineContext: CoroutineContext,
    onProgress: ((Int) -> Unit),
  ) {
    val subChunked = chunk.chunked(1000)
    var progress: Int = 0
    val params = getParams(job)
    subChunked.forEach { subChunk ->
      coroutineContext.ensureActive()
      translationService.copyBatch(subChunk, params.sourceLanguageId, params.targetLanguageIds)
      entityManager.flush()
      progress += subChunk.size
      onProgress.invoke(progress)
    }
  }

  override fun getParamsType(): Class<CopyTranslationJobParams> {
    return CopyTranslationJobParams::class.java
  }

  override fun getTarget(data: CopyTranslationRequest): List<Long> {
    return data.keyIds
  }

  override fun getTargetItemType(): Class<Long> {
    return Long::class.java
  }

  override fun getParams(data: CopyTranslationRequest): CopyTranslationJobParams {
    return CopyTranslationJobParams().apply {
      sourceLanguageId = data.sourceLanguageId
      targetLanguageIds = data.targetLanguageIds
    }
  }
}
