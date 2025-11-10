package io.tolgee.batch.processors

import io.tolgee.batch.ChunkProcessor
import io.tolgee.batch.data.BatchJobDto
import io.tolgee.batch.request.LabelTranslationsRequest
import io.tolgee.model.batch.params.TranslationLabelParams
import jakarta.persistence.EntityManager
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.CoroutineContext

abstract class AbstractTranslationLabelChunkProcessor(
  private val entityManager: EntityManager,
) : ChunkProcessor<LabelTranslationsRequest, TranslationLabelParams, Long> {
  override fun process(
    job: BatchJobDto,
    chunk: List<Long>,
    coroutineContext: CoroutineContext,
    onProgress: (Int) -> Unit,
  ) {
    val subChunked = chunk.chunked(100)
    val params = getParams(job)
    var progress = 0

    subChunked.forEach { subChunk ->
      coroutineContext.ensureActive()

      process(subChunk, params.languageIds, params.labelIds)

      entityManager.flush()
      progress += subChunk.size
      onProgress(progress)
    }
  }

  override fun getTarget(data: LabelTranslationsRequest): List<Long> {
    return data.keyIds
  }

  override fun getParamsType(): Class<TranslationLabelParams> {
    return TranslationLabelParams::class.java
  }

  override fun getTargetItemType(): Class<Long> {
    return Long::class.java
  }

  override fun getParams(data: LabelTranslationsRequest): TranslationLabelParams {
    return TranslationLabelParams(
      labelIds = data.labelIds,
      languageIds = data.languageIds,
    )
  }

  abstract fun process(
    subChunk: List<Long>,
    languageIds: List<Long>,
    labelIds: List<Long>,
  )
}
