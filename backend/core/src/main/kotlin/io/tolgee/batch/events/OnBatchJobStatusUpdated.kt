package io.tolgee.batch.events

import io.tolgee.model.batch.BatchJobStatus

class OnBatchJobStatusUpdated(
  val jobId: Long,
  val projectId: Long?,
  val status: BatchJobStatus,
)
