package io.tolgee.hateoas.notification

import io.tolgee.model.notifications.NotificationDigestFrequency
import org.springframework.hateoas.RepresentationModel
import java.io.Serializable

data class NotificationSettingModel(
  var accountSecurity: NotificationSettingGroupModel,
  var tasks: NotificationSettingGroupModel,
  var localization: List<NotificationTypeSettingModel>,
  var digestFrequency: NotificationDigestFrequency,
) : RepresentationModel<NotificationSettingModel>(),
  Serializable
