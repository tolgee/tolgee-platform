package io.tolgee.batch.events

import io.tolgee.batch.BatchJobDto
import io.tolgee.batch.OnBatchOperationCompleted

data class OnBatchOperationFailed(
  override val job: BatchJobDto,
) : OnBatchOperationCompleted
