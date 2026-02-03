package io.tolgee.websocket

import io.tolgee.activity.data.ActivityType

data class WebsocketEvent(
  val actor: ActorInfo? = null,
  val data: Any? = null,
  val sourceActivity: ActivityType? = null,
  val activityId: Long? = null,
  val dataCollapsed: Boolean = false,
  val timestamp: Long = System.currentTimeMillis(),
)
