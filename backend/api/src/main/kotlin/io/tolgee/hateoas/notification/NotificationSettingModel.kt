package io.tolgee.hateoas.notification

import io.tolgee.model.notifications.NotificationChannel
import io.tolgee.model.notifications.NotificationTypeGroup
import org.springframework.hateoas.RepresentationModel
import java.io.Serializable

data class NotificationSettingModel(
  var items: Map<NotificationTypeGroup, Map<NotificationChannel, Boolean>>
) : RepresentationModel<NotificationSettingModel>(), Serializable
