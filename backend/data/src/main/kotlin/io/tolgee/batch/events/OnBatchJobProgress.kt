package io.tolgee.batch.events

import io.tolgee.batch.BatchApiPhase
import io.tolgee.batch.data.BatchJobDto

data class OnBatchJobProgress(
  val job: BatchJobDto,
  val processed: Long,
  val total: Long,
  val batchApiPhase: BatchApiPhase? = null,
)
