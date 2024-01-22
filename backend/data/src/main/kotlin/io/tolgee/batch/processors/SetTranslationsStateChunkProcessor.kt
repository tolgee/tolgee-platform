package io.tolgee.batch.processors

import io.tolgee.batch.ChunkProcessor
import io.tolgee.batch.data.BatchJobDto
import io.tolgee.batch.request.SetTranslationsStateStateRequest
import io.tolgee.model.batch.params.SetTranslationStateJobParams
import io.tolgee.service.translation.TranslationService
import jakarta.persistence.EntityManager
import kotlinx.coroutines.ensureActive
import org.springframework.stereotype.Component
import kotlin.coroutines.CoroutineContext

@Component
class SetTranslationsStateChunkProcessor(
  private val translationService: TranslationService,
  private val entityManager: EntityManager,
) : ChunkProcessor<SetTranslationsStateStateRequest, SetTranslationStateJobParams, Long> {
  override fun process(
    job: BatchJobDto,
    chunk: List<Long>,
    coroutineContext: CoroutineContext,
    onProgress: (Int) -> Unit,
  ) {
    val subChunked = chunk.chunked(1000)
    var progress: Int = 0
    val params = getParams(job)
    subChunked.forEach { subChunk ->
      coroutineContext.ensureActive()
      params.state?.let { translationService.setStateBatch(subChunk, params.languageIds, it) }
      entityManager.flush()
      progress += subChunk.size
      onProgress.invoke(progress)
    }
  }

  override fun getTarget(data: SetTranslationsStateStateRequest): List<Long> {
    return data.keyIds
  }

  override fun getParamsType(): Class<SetTranslationStateJobParams> {
    return SetTranslationStateJobParams::class.java
  }

  override fun getTargetItemType(): Class<Long> {
    return Long::class.java
  }

  override fun getParams(data: SetTranslationsStateStateRequest): SetTranslationStateJobParams {
    return SetTranslationStateJobParams().apply {
      languageIds = data.languageIds
      state = data.state
    }
  }
}
