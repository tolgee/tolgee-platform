package io.tolgee.batch

data class ExecutionQueueItem(
  val chunkExecutionId: Long,
  val executeAfter: Long?
)
