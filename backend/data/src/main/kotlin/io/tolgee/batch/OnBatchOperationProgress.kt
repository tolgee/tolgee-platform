package io.tolgee.batch

import io.tolgee.model.batch.BatchJob

data class OnBatchOperationProgress(
  val job: BatchJob,
  val processed: Long,
  val total: Long,
)
