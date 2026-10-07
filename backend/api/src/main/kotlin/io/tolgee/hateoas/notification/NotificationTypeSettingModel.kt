package io.tolgee.hateoas.notification

import io.tolgee.model.notifications.NotificationType
import java.io.Serializable

data class NotificationTypeSettingModel(
  var type: NotificationType,
  var inApp: Boolean,
  var email: Boolean,
) : Serializable
