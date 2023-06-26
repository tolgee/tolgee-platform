package io.tolgee.batch

data class OnBatchOperationSucceeded(
  override val job: BatchJobDto,
) : OnBatchOperationCompleted
