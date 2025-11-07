package io.tolgee.batch.request

import io.tolgee.activity.data.ActivityType

data class ActivityNotificationRequest(
  val projectId: Long,
  val originatingUserId: Long,
  val entityId: Long,
  val activityType: ActivityType,
)
