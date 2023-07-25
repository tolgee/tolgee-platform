package io.tolgee.batch.processors

import io.tolgee.batch.BatchJobDto
import io.tolgee.batch.ChunkProcessor
import io.tolgee.batch.request.TagKeysRequest
import io.tolgee.model.EntityWithId
import io.tolgee.model.batch.BatchJob
import io.tolgee.model.batch.TagKeysParams
import io.tolgee.service.key.TagService
import kotlinx.coroutines.ensureActive
import org.springframework.stereotype.Component
import javax.persistence.EntityManager
import kotlin.coroutines.CoroutineContext

@Component
class TagKeysChunkProcessor(
  private val entityManager: EntityManager,
  private val tagService: TagService
) : ChunkProcessor<TagKeysRequest> {
  override fun process(
    job: BatchJobDto,
    chunk: List<Long>,
    coroutineContext: CoroutineContext,
    onProgress: ((Int) -> Unit)
  ) {
    val subChunked = chunk.chunked(100)
    var progress: Int = 0
    var params = getParams(job)
    subChunked.forEach { subChunk ->
      coroutineContext.ensureActive()
      tagService.tagKeysById(subChunk.associateWith { params.tags })
      entityManager.flush()
      progress += subChunk.size
      onProgress.invoke(progress)
    }
  }

  override fun getTarget(data: TagKeysRequest): List<Long> {
    return data.keyIds
  }

  private fun getParams(job: BatchJobDto): TagKeysParams {
    return entityManager.createQuery(
      """from TagKeysParams tkp where tkp.batchJob.id = :batchJobId""",
      TagKeysParams::class.java
    ).setParameter("batchJobId", job.id).singleResult
      ?: throw IllegalStateException("No params found")
  }

  override fun getParams(data: TagKeysRequest, job: BatchJob): EntityWithId? {
    return TagKeysParams().apply {
      this.batchJob = job
      this.tags = data.tags
    }
  }
}
