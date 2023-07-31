package io.tolgee.batch

data class ExecutionQueueItem(
  val chunkExecutionId: Long,
  val jobId: Long,
  val executeAfter: Long?,
  val jobCharacter: JobCharacter,
  var managementErrorRetrials: Int = 0,
)
