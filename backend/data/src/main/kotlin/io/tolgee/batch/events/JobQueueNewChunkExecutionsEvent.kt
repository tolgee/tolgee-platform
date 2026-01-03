package io.tolgee.batch.events

import io.tolgee.batch.data.BatchJobChunkExecutionDto

data class JobQueueNewChunkExecutionsEvent(
  val items: List<BatchJobChunkExecutionDto>,
)
