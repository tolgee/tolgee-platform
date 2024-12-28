package io.tolgee.batch

import io.tolgee.batch.data.BatchJobType

class BatchOperationParams(
  val type: BatchJobType,
  val projectId: Long?,
  val target: List<Any>,
  val request: Any?,
)
