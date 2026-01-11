package io.tolgee.batch.data

import io.tolgee.batch.JobCharacter

data class ExecutionQueueItem(
  val chunkExecutionId: Long,
  val jobId: Long,
  var executeAfter: Long?,
  val jobCharacter: JobCharacter,
  var managementErrorRetrials: Int = 0,
)
