package io.tolgee.batch

import io.tolgee.batch.data.BatchJobDto
import tools.jackson.databind.ObjectMapper

abstract class AbstractChunkProcessor<RequestType, ParamsType, TargetItemType>(
  private val objectMapper: ObjectMapper,
) : ChunkProcessor<RequestType, ParamsType, TargetItemType> {
  override fun getParams(job: BatchJobDto): ParamsType {
    return objectMapper.convertValue(job.params, getParamsType())
  }
}
