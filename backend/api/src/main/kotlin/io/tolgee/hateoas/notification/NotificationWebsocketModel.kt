package io.tolgee.hateoas.notification

data class NotificationWebsocketModel(
  val currentlyUnseenCount: Int,
  val newNotification: NotificationModel?,
)
