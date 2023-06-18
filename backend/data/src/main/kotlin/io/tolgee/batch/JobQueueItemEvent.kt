package io.tolgee.batch

data class JobQueueItemEvent(
  val item: ExecutionQueueItem,
  val type: QueueItemType
)
