package io.tolgee.batch

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlin.coroutines.CoroutineContext

interface ChunkProcessor<RequestType, ParamsType, TargetItemType> {
  fun process(
    job: BatchJobDto,
    chunk: List<TargetItemType>,
    coroutineContext: CoroutineContext,
    onProgress: ((Int) -> Unit)
  )

  fun getTarget(data: RequestType): List<TargetItemType>
  fun getParams(data: RequestType): ParamsType

  fun getParams(job: BatchJobDto): ParamsType {
    return jacksonObjectMapper().convertValue(job.params, getParamsType())
  }

  fun getParamsType(): Class<ParamsType>?

  fun getTargetItemType(): Class<TargetItemType>
}
