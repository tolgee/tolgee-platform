package io.tolgee.batch

import io.tolgee.model.EntityWithId
import io.tolgee.model.batch.BatchJob

interface ChunkProcessor<RequestType> {
  fun process(job: BatchJobDto, chunk: List<Long>, onProgress: ((Int) -> Unit))
  fun getTarget(data: RequestType): List<Long>
  fun getParams(data: RequestType, job: BatchJob): EntityWithId?
}
