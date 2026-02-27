package io.tolgee.batch.processors

import io.tolgee.batch.ChunkProcessor
import io.tolgee.batch.ProgressManager
import io.tolgee.batch.data.BatchJobDto
import io.tolgee.batch.request.NoOpRequest
import org.springframework.stereotype.Component
import kotlin.coroutines.CoroutineContext

data class NoOpParams(
  val chunkProcessingDelayMs: Long = 0,
)

@Component
class NoOpChunkProcessor(
  private val progressManager: ProgressManager,
) : ChunkProcessor<NoOpRequest, NoOpParams, Long> {
  override fun process(
    job: BatchJobDto,
    chunk: List<Long>,
    coroutineContext: CoroutineContext,
  ) {
    val params = getParams(job)
    if (params.chunkProcessingDelayMs > 0) {
      Thread.sleep(params.chunkProcessingDelayMs)
    }
    progressManager.reportSingleChunkProgress(job.id, chunk.size)
  }

  override fun getParamsType(): Class<NoOpParams> {
    return NoOpParams::class.java
  }

  override fun getParams(data: NoOpRequest): NoOpParams {
    return NoOpParams(chunkProcessingDelayMs = data.chunkProcessingDelayMs)
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
