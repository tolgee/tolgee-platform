package io.tolgee.batch

import io.tolgee.model.batch.BatchJob

data class OnBatchOperationSucceeded(
  override val job: BatchJob,
) : OnBatchOperationCompleted
