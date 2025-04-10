package io.tolgee.batch

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.tolgee.batch.data.BatchJobDto
import java.util.*
import kotlin.coroutines.CoroutineContext

interface ChunkProcessor<RequestType, ParamsType, TargetItemType> {
  fun process(
    job: BatchJobDto,
    chunk: List<TargetItemType>,
    coroutineContext: CoroutineContext,
    onProgress: ((Int) -> Unit),
  )

  fun getTarget(data: RequestType): List<TargetItemType>

  fun getExecuteAfter(data: RequestType): Date? = null

  fun getParams(data: RequestType): ParamsType

  fun getParams(job: BatchJobDto): ParamsType = jacksonObjectMapper().convertValue(job.params, getParamsType())

  fun getMaxPerJobConcurrency(): Int = -1

  fun getJobCharacter(): JobCharacter = JobCharacter.FAST

  fun getChunkSize(
    request: RequestType,
    projectId: Long?,
  ): Int = 0

  fun getParamsType(): Class<ParamsType>?

  fun getTargetItemType(): Class<TargetItemType>
}
