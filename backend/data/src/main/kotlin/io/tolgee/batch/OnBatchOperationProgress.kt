package io.tolgee.batch

data class OnBatchOperationProgress(
  val job: BatchJobDto,
  val processed: Long,
  val total: Long,
)
