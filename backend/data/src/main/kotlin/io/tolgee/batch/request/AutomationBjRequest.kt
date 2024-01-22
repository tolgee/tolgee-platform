package io.tolgee.batch.request

data class AutomationBjRequest(
  var triggerId: Long,
  var actionId: Long,
  var activityRevisionId: Long?,
)
