package io.tolgee.batch

data class JobQueueItemsEvent(
  val items: List<ExecutionQueueItem>,
  val type: QueueEventType
)
