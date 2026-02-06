package io.tolgee.batch

import io.tolgee.model.batch.BatchJobStatus

data class WebsocketProgressInfo(
  val jobId: Long,
  val processed: Long?,
  val total: Long?,
  val status: BatchJobStatus,
  val errorMessage: String? = null,
  val batchApiPhase: BatchApiPhase? = null,
)

enum class BatchApiPhase {
  SUBMITTING,
  WAITING_FOR_OPENAI,
  APPLYING_RESULTS,
}
