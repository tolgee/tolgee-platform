package io.tolgee.batch.processors

import io.tolgee.batch.ChunkProcessor
import io.tolgee.batch.data.BatchJobDto
import io.tolgee.batch.request.DeleteKeysRequest
import io.tolgee.service.key.KeyService
import jakarta.persistence.EntityManager
import kotlinx.coroutines.ensureActive
import org.springframework.stereotype.Component
import kotlin.coroutines.CoroutineContext

@Component
class DeleteKeysChunkProcessor(
  private val keyService: KeyService,
  private val entityManager: EntityManager,
) : ChunkProcessor<DeleteKeysRequest, Any?, Long> {
  override fun process(
    job: BatchJobDto,
    chunk: List<Long>,
    coroutineContext: CoroutineContext,
    onProgress: ((Int) -> Unit),
  ) {
    coroutineContext.ensureActive()
    val subChunked = chunk.chunked(100)
    var progress: Int = 0
    subChunked.forEach { subChunk ->
      coroutineContext.ensureActive()
      @Suppress("UNCHECKED_CAST")
      keyService.deleteMultiple(subChunk)
      entityManager.flush()
      progress += subChunk.size
      onProgress.invoke(progress)
    }
  }

  override fun getParamsType(): Class<Any?>? {
    return null
  }

  override fun getParams(data: DeleteKeysRequest): Any? {
    return null
  }

  override fun getTargetItemType(): Class<Long> {
    return Long::class.java
  }

  override fun getTarget(data: DeleteKeysRequest): List<Long> {
    return data.keyIds
  }
}
