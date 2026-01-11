package io.tolgee.batch.events

import io.tolgee.model.batch.BatchJob
import io.tolgee.model.batch.BatchJobChunkExecution

data class OnBatchJobCreated(
  val job: BatchJob,
  val executions: List<BatchJobChunkExecution>,
)
