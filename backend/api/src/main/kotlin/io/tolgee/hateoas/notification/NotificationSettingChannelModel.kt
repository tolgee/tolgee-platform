package io.tolgee.hateoas.notification

import io.tolgee.model.notifications.NotificationChannel
import org.springframework.hateoas.RepresentationModel
import java.io.Serializable

data class NotificationSettingChannelModel(
  var channel: NotificationChannel,
  var enabled: Boolean,
) : RepresentationModel<NotificationSettingChannelModel>(),
  Serializable
