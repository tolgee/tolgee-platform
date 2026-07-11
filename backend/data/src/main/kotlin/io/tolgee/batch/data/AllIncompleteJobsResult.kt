package io.tolgee.batch.data

import io.tolgee.model.batch.BatchJobStatus

data class AllIncompleteJobsResult(
  val jobId: Long,
  val status: BatchJobStatus,
  val totalChunks: Int,
)
