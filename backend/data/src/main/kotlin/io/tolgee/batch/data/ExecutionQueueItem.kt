package io.tolgee.batch.data

import io.tolgee.batch.JobCharacter

data class ExecutionQueueItem(
  val chunkExecutionId: Long,
  val jobId: Long,
  var executeAfter: Long?,
  val jobCharacter: JobCharacter,
  var managementErrorRetrials: Int = 0,
  // Grouping key for type-fair round-robin (see BatchJobChunkExecutionQueue.pollRoundRobin).
  // Defaulted ONLY so a queue event serialized by an older instance (without this field)
  // still deserializes during a rolling deploy; the 60s populateQueue() re-read then corrects
  // the grouping from the DB. Every in-process construction path sets it explicitly.
  val jobType: BatchJobType = BatchJobType.NO_OP,
)
