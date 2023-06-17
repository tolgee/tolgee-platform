package io.tolgee.batch

import io.tolgee.model.EntityWithId
import io.tolgee.model.batch.BatchJob

interface ChunkProcessor<RequestType> {
  fun process(job: BatchJob, chunk: List<Long>)
  fun getTarget(data: RequestType): List<Long>
  fun getParams(data: RequestType, job: BatchJob): EntityWithId
}
