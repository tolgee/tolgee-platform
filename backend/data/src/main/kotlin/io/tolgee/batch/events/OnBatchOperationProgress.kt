package io.tolgee.batch.events

import io.tolgee.batch.BatchJobDto

data class OnBatchOperationProgress(
  val job: BatchJobDto,
  val processed: Long,
  val total: Long,
)
