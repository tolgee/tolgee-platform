package io.tolgee.batch.events

import io.tolgee.batch.OnBatchJobCompleted
import io.tolgee.batch.data.BatchJobDto

data class OnBatchJobCancelled(
  override val job: BatchJobDto,
) : OnBatchJobCompleted
