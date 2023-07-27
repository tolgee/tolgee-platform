package io.tolgee.batch.processors

import io.tolgee.batch.BatchJobDto
import io.tolgee.batch.ChunkProcessor
import io.tolgee.batch.request.UntagKeysRequest
import io.tolgee.model.batch.params.UntagKeysParams
import io.tolgee.service.key.TagService
import kotlinx.coroutines.ensureActive
import org.springframework.stereotype.Component
import javax.persistence.EntityManager
import kotlin.coroutines.CoroutineContext

@Component
class UntagKeysChunkProcessor(
  private val entityManager: EntityManager,
  private val tagService: TagService
) : ChunkProcessor<UntagKeysRequest, UntagKeysParams> {
  override fun process(
    job: BatchJobDto,
    chunk: List<Long>,
    coroutineContext: CoroutineContext,
    onProgress: ((Int) -> Unit)
  ) {
    val subChunked = chunk.chunked(100)
    var progress = 0
    val params = getParams(job)
    subChunked.forEach { subChunk ->
      coroutineContext.ensureActive()
      tagService.untagKeys(subChunk.associateWith { params.tags })
      entityManager.flush()
      progress += subChunk.size
      onProgress.invoke(progress)
    }
  }

  override fun getTarget(data: UntagKeysRequest): List<Long> {
    return data.keyIds
  }

  override fun getParamsType(): Class<UntagKeysParams> {
    return UntagKeysParams::class.java
  }

  override fun getParams(data: UntagKeysRequest): UntagKeysParams {
    return UntagKeysParams().apply {
      this.tags = data.tags
    }
  }
}
