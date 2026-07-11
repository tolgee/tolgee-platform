package io.tolgee.batch.events

import io.tolgee.batch.data.BatchJobDto

data class OnBatchJobStarted(
  val job: BatchJobDto,
)
