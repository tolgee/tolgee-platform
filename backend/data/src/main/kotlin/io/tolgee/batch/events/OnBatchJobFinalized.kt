package io.tolgee.batch.events

import io.tolgee.batch.OnBatchJobCompleted
import io.tolgee.batch.data.BatchJobDto

/** [activityRevisionId] is null when the job modified nothing activity-logged. */
data class OnBatchJobFinalized(
  override val job: BatchJobDto,
  val activityRevisionId: Long?,
) : OnBatchJobCompleted
