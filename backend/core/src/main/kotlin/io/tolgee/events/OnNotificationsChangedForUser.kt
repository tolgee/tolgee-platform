package io.tolgee.events

import io.tolgee.model.notifications.Notification

class OnNotificationsChangedForUser(
  val userId: Long,
  val newNotification: Notification? = null,
)
