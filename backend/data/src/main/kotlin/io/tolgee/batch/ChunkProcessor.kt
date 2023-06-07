package io.tolgee.batch

import io.tolgee.model.EntityWithId
import io.tolgee.model.batch.BatchJob

interface ChunkProcessor {
  fun process(job: BatchJob, chunk: List<Long>)
  fun getTarget(data: Any): List<Long>
  fun getParams(data: Any, job: BatchJob): EntityWithId
}
