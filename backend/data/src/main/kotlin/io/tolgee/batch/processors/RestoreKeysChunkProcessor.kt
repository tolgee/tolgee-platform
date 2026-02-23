package io.tolgee.batch.processors

import io.tolgee.batch.ChunkProcessor
import io.tolgee.batch.ProgressManager
import io.tolgee.batch.data.BatchJobDto
import io.tolgee.batch.request.RestoreKeysRequest
import io.tolgee.service.key.KeyService
import jakarta.persistence.EntityManager
import kotlinx.coroutines.ensureActive
import org.springframework.stereotype.Component
import kotlin.coroutines.CoroutineContext

@Component
class RestoreKeysChunkProcessor(
  private val keyService: KeyService,
  private val entityManager: EntityManager,
  private val progressManager: ProgressManager,
) : ChunkProcessor<RestoreKeysRequest, Any?, Long> {
  override fun process(
    job: BatchJobDto,
    chunk: List<Long>,
    coroutineContext: CoroutineContext,
  ) {
    coroutineContext.ensureActive()
    val subChunked = chunk.chunked(100)
    subChunked.forEach { subChunk ->
      coroutineContext.ensureActive()
      keyService.restoreKeys(subChunk)
      entityManager.flush()
      progressManager.reportSingleChunkProgress(job.id, subChunk.size)
    }
  }

  override fun getParamsType(): Class<Any?>? {
    return null
  }

  override fun getParams(data: RestoreKeysRequest): Any? {
    return null
  }

  override fun getTargetItemType(): Class<Long> {
    return Long::class.java
  }

  override fun getTarget(data: RestoreKeysRequest): List<Long> {
    return data.keyIds
  }
}
