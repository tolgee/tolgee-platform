package io.tolgee.batch.processors

import com.fasterxml.jackson.databind.ObjectMapper
import io.tolgee.batch.AbstractChunkProcessor
import io.tolgee.batch.ProgressManager
import io.tolgee.batch.data.BatchJobDto
import io.tolgee.batch.request.NoOpRequest
import org.springframework.stereotype.Component
import kotlin.coroutines.CoroutineContext

@Component
class NoOpChunkProcessor(
  private val progressManager: ProgressManager,
  objectMapper: ObjectMapper,
) : AbstractChunkProcessor<NoOpRequest, Any?, Long>(objectMapper) {
  override fun process(
    job: BatchJobDto,
    chunk: List<Long>,
    coroutineContext: CoroutineContext,
  ) {
    // Report progress for the whole chunk at once
    progressManager.reportSingleChunkProgress(job.id, chunk.size)
  }

  override fun getParamsType(): Class<Any?>? {
    return null
  }

  override fun getParams(data: NoOpRequest): Any? {
    return null
  }

  override fun getTargetItemType(): Class<Long> {
    return Long::class.java
  }

  override fun getTarget(data: NoOpRequest): List<Long> {
    return data.itemIds
  }

  override fun getChunkSize(
    request: NoOpRequest,
    projectId: Long?,
  ): Int {
    return 1
  }
}
