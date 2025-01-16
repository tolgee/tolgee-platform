package io.tolgee.events

import io.tolgee.model.Notification

class OnNotificationsChangedForUser(
  val userId: Long,
  val newNotification: Notification? = null,
)
