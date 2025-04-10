package io.tolgee.hateoas.notification

import org.springframework.hateoas.RepresentationModel
import java.io.Serializable

data class NotificationSettingModel(
  var accountSecurity: NotificationSettingGroupModel,
  var tasks: NotificationSettingGroupModel,
) : RepresentationModel<NotificationSettingModel>(),
  Serializable
