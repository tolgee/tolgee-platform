package io.tolgee.batch.events

import io.tolgee.batch.data.ExecutionQueueItem
import io.tolgee.batch.data.QueueEventType

data class JobQueueItemsEvent(
  val items: List<ExecutionQueueItem>,
  val type: QueueEventType,
)
