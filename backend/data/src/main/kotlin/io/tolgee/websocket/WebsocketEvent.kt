package io.tolgee.websocket

import io.tolgee.activity.data.ActivityType

data class WebsocketEvent(
  val actor: ActorInfo,
  val data: Any? = null,
  val sourceActivity: ActivityType?,
  val activityId: Long?,
  val dataCollapsed: Boolean,
  val timestamp: Long,
)
