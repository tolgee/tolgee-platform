package io.tolgee.hateoas.notification

import io.tolgee.service.notification.NotificationModel

data class NotificationResponse(
  val notifications: List<NotificationModel>,
)
