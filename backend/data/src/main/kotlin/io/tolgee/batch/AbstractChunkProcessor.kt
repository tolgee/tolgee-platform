package io.tolgee.batch

import com.fasterxml.jackson.databind.ObjectMapper
import io.tolgee.batch.data.BatchJobDto

abstract class AbstractChunkProcessor<RequestType, ParamsType, TargetItemType>(
  private val objectMapper: ObjectMapper,
) : ChunkProcessor<RequestType, ParamsType, TargetItemType> {
  override fun getParams(job: BatchJobDto): ParamsType {
    return objectMapper.convertValue(job.params, getParamsType())
  }
}
