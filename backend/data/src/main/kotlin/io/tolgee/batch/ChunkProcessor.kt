package io.tolgee.batch

import io.tolgee.batch.data.BatchJobDto
import java.util.Date
import kotlin.coroutines.CoroutineContext

interface ChunkProcessor<RequestType, ParamsType, TargetItemType> {
  fun process(
    job: BatchJobDto,
    chunk: List<TargetItemType>,
    coroutineContext: CoroutineContext,
  )

  fun getTarget(data: RequestType): List<TargetItemType>

  fun getExecuteAfter(data: RequestType): Date? {
    return null
  }

  fun getParams(data: RequestType): ParamsType

  fun getParams(job: BatchJobDto): ParamsType

  fun getMaxPerJobConcurrency(): Int {
    return -1
  }

  fun getJobCharacter(
    request: RequestType,
    projectId: Long?,
  ): JobCharacter {
    return JobCharacter.FAST
  }

  fun getChunkSize(
    request: RequestType,
    projectId: Long?,
  ): Int {
    return 0
  }

  fun getParamsType(): Class<ParamsType>?

  fun getTargetItemType(): Class<TargetItemType>
}
