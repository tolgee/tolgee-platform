package io.tolgee.batch

import io.tolgee.model.batch.BatchJobStatus

data class WebsocketProgressInfo(
  val jobId: Long,
  val processed: Long?,
  val total: Long?,
  val status: BatchJobStatus,
  val errorMessage: String? = null,
)
