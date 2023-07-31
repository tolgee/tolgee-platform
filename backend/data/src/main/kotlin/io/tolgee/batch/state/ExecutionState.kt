package io.tolgee.batch.state

import io.tolgee.model.batch.BatchJobChunkExecutionStatus

data class ExecutionState(
  var successTargets: List<Any>,
  var status: BatchJobChunkExecutionStatus,
  var chunkNumber: Int?,
  var retry: Boolean?,
  var transactionCommitted: Boolean,
)
