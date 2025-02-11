package io.tolgee.hateoas.notification

import io.tolgee.model.notifications.NotificationTypeGroup
import org.springframework.hateoas.RepresentationModel
import java.io.Serializable

data class NotificationSettingGroupModel(
  var group: NotificationTypeGroup,
  var channels: List<NotificationSettingChannelModel>,
) : RepresentationModel<NotificationSettingGroupModel>(), Serializable
