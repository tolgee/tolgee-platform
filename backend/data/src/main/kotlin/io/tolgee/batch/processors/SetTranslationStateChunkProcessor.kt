package io.tolgee.batch.processors

import io.tolgee.batch.BatchJobDto
import io.tolgee.batch.ChunkProcessor
import io.tolgee.batch.request.SetStateRequest
import io.tolgee.model.batch.BatchJob
import io.tolgee.model.batch.SetTranslationStateJobParams
import io.tolgee.service.translation.TranslationService
import kotlinx.coroutines.ensureActive
import org.springframework.stereotype.Component
import javax.persistence.EntityManager
import kotlin.coroutines.CoroutineContext

@Component
class SetTranslationStateChunkProcessor(
  private val translationService: TranslationService,
  private val entityManager: EntityManager
) : ChunkProcessor<SetStateRequest> {
  override fun process(
    job: BatchJobDto,
    chunk: List<Long>,
    coroutineContext: CoroutineContext,
    onProgress: ((Int) -> Unit)
  ) {
    val subChunked = chunk.chunked(1000)
    var progress: Int = 0
    val params = getParams(job)
    subChunked.forEach { subChunk ->
      coroutineContext.ensureActive()
      params.state?.let { translationService.setState(subChunk, params.languageIds, it) }
      entityManager.flush()
      progress += subChunk.size
      onProgress.invoke(progress)
    }
  }

  private fun getParams(job: BatchJobDto): SetTranslationStateJobParams {
    return entityManager.createQuery(
      """from SetTranslationStateJobParams tjp where tjp.batchJob.id = :batchJobId""",
      SetTranslationStateJobParams::class.java
    )
      .setParameter("batchJobId", job.id).singleResult
      ?: throw IllegalStateException("No params found")
  }

  override fun getTarget(data: SetStateRequest): List<Long> {
    return data.keyIds
  }

  override fun getParams(data: SetStateRequest, job: BatchJob): SetTranslationStateJobParams {
    return SetTranslationStateJobParams().apply {
      batchJob = job
      languageIds = data.languageIds
      state = data.state
    }
  }
}
