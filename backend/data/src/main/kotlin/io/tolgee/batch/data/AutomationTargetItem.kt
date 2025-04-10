package io.tolgee.batch.data

data class AutomationTargetItem(
  val triggerId: Long,
  val actionId: Long,
  val activityRevisionId: Long?,
)
