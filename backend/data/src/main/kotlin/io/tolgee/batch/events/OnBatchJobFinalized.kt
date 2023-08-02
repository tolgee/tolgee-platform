package io.tolgee.batch.events

import io.tolgee.batch.BatchJobDto
import io.tolgee.batch.OnBatchJobCompleted

data class OnBatchJobFinalized(
  override val job: BatchJobDto,
) : OnBatchJobCompleted
