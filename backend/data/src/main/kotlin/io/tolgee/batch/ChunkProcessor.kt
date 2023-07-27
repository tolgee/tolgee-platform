package io.tolgee.batch

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlin.coroutines.CoroutineContext

interface ChunkProcessor<RequestType, ParamsType> {
  fun process(job: BatchJobDto, chunk: List<Long>, coroutineContext: CoroutineContext, onProgress: ((Int) -> Unit))
  fun getTarget(data: RequestType): List<Long>
  fun getParams(data: RequestType): ParamsType

  fun getParams(job: BatchJobDto): ParamsType {
    return jacksonObjectMapper().convertValue(job.params, getParamsType())
  }

  fun getParamsType(): Class<ParamsType>?
}
