package io.tolgee.batch

data class OnBatchOperationFailed(
  override val job: BatchJobDto,
) : OnBatchOperationCompleted
