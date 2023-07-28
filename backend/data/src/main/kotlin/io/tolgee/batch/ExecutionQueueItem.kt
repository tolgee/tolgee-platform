package io.tolgee.batch

data class ExecutionQueueItem(
  val chunkExecutionId: Long,
  val jobId: Long,
  var executeAfter: Long?,
  var managementErrorRetrials: Int = 0,
)
