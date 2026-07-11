package io.tolgee.batch.processors

import com.fasterxml.jackson.databind.ObjectMapper
import io.tolgee.batch.AbstractChunkProcessor
import io.tolgee.batch.ProgressManager
import io.tolgee.batch.data.BatchJobDto
import io.tolgee.batch.request.DeleteKeysRequest
import io.tolgee.service.key.KeyService
import io.tolgee.service.security.UserAccountService
import jakarta.persistence.EntityManager
import kotlinx.coroutines.ensureActive
import org.springframework.stereotype.Component
import kotlin.coroutines.CoroutineContext

@Component
class DeleteKeysChunkProcessor(
  private val keyService: KeyService,
  private val entityManager: EntityManager,
  private val progressManager: ProgressManager,
  private val userAccountService: UserAccountService,
  objectMapper: ObjectMapper,
) : AbstractChunkProcessor<DeleteKeysRequest, Any?, Long>(objectMapper) {
  override fun process(
    job: BatchJobDto,
    chunk: List<Long>,
    coroutineContext: CoroutineContext,
  ) {
    coroutineContext.ensureActive()
    val author = job.authorId?.let { userAccountService.findActive(it) }
    val subChunked = chunk.chunked(100)
    subChunked.forEach { subChunk ->
      coroutineContext.ensureActive()
      @Suppress("UNCHECKED_CAST")
      keyService.softDeleteMultiple(subChunk, deletedBy = author)
      entityManager.flush()
      progressManager.reportSingleChunkProgress(job.id, subChunk.size)
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

  override fun getChunkSize(
    request: DeleteKeysRequest,
    projectId: Long?,
  ): Int = 5000
}
