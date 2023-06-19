package io.tolgee.batch

import io.tolgee.model.batch.BatchJob

data class OnBatchOperationFailed(
  override val job: BatchJob,
) : OnBatchOperationCompleted
