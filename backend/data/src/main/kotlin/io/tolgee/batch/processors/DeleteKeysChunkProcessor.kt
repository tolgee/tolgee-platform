package io.tolgee.batch.processors

import io.tolgee.batch.ChunkProcessor
import io.tolgee.batch.request.DeleteKeysRequest
import io.tolgee.model.EntityWithId
import io.tolgee.model.batch.BatchJob
import io.tolgee.service.key.KeyService
import org.springframework.stereotype.Component
import javax.persistence.EntityManager

@Component
class DeleteKeysChunkProcessor(
  private val keyService: KeyService,
  private val entityManager: EntityManager
) : ChunkProcessor<DeleteKeysRequest> {
  override fun process(job: BatchJob, chunk: List<Long>, onProgress: ((Int) -> Unit)) {
    val subChunked = chunk.chunked(100)
    var progress: Int = 0
    subChunked.forEachIndexed { index, subChunk ->
      keyService.deleteMultiple(subChunk)
      entityManager.flush()
      progress += subChunk.size
      onProgress.invoke(progress)
      return
    }
  }

  override fun getTarget(data: DeleteKeysRequest): List<Long> {
    return data.keyIds
  }

  override fun getParams(data: DeleteKeysRequest, job: BatchJob): EntityWithId? {
    return null
  }
}
