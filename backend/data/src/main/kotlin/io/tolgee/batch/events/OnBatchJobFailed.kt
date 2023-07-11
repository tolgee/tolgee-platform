package io.tolgee.batch.events

import io.tolgee.batch.BatchJobDto
import io.tolgee.batch.OnBatchJobCompleted
import io.tolgee.constants.Message

data class OnBatchJobFailed(
  override val job: BatchJobDto,
  val errorMessage: Message?,
) : OnBatchJobCompleted
