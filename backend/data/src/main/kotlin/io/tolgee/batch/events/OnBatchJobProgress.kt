package io.tolgee.batch.events

import io.tolgee.batch.BatchJobDto

data class OnBatchJobProgress(
  val job: BatchJobDto,
  val processed: Long,
  val total: Long,
)
