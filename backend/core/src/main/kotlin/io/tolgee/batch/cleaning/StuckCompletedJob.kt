package io.tolgee.batch.cleaning

import io.tolgee.model.batch.BatchJobChunkExecutionStatus

data class StuckCompletedJob(
  val id: Long,
  val projectId: Long?,
  val statuses: List<BatchJobChunkExecutionStatus>,
)
