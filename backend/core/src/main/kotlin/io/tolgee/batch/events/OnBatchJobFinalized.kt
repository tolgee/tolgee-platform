package io.tolgee.batch.events

import io.tolgee.batch.OnBatchJobCompleted
import io.tolgee.batch.data.BatchJobDto

data class OnBatchJobFinalized(
  override val job: BatchJobDto,
  val activityRevisionId: Long,
) : OnBatchJobCompleted
