package io.tolgee.batch.processors

import io.tolgee.batch.BatchJobDto
import io.tolgee.batch.ChunkProcessor
import io.tolgee.batch.request.ClearTranslationsRequest
import io.tolgee.model.batch.BatchJob
import io.tolgee.model.batch.ClearTranslationsJobParams
import io.tolgee.service.translation.TranslationService
import kotlinx.coroutines.ensureActive
import org.springframework.stereotype.Component
import javax.persistence.EntityManager
import kotlin.coroutines.CoroutineContext

@Component
class ClearTranslationsChunkProcessor(
  private val translationService: TranslationService,
  private val entityManager: EntityManager
) : ChunkProcessor<ClearTranslationsRequest> {
  override fun process(
    job: BatchJobDto,
    chunk: List<Long>,
    coroutineContext: CoroutineContext,
    onProgress: ((Int) -> Unit)
  ) {
    val subChunked = chunk.chunked(100)
    var progress: Int = 0
    val params = getParams(job)
    subChunked.forEach { subChunk ->
      coroutineContext.ensureActive()
      translationService.clear(subChunk, params.languageIds)
      entityManager.flush()
      progress += subChunk.size
      onProgress.invoke(progress)
    }
  }

  private fun getParams(job: BatchJobDto): ClearTranslationsJobParams {
    return entityManager.createQuery(
      """from ClearTranslationsJobParams ctjp where ctjp.batchJob.id = :batchJobId""",
      ClearTranslationsJobParams::class.java
    )
      .setParameter("batchJobId", job.id).singleResult
      ?: throw IllegalStateException("No params found")
  }

  override fun getTarget(data: ClearTranslationsRequest): List<Long> {
    return data.keyIds
  }

  override fun getParams(data: ClearTranslationsRequest, job: BatchJob): ClearTranslationsJobParams {
    return ClearTranslationsJobParams().apply {
      batchJob = job
      languageIds = data.languageIds
    }
  }
}
