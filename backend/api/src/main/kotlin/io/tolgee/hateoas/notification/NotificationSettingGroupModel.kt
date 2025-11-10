package io.tolgee.hateoas.notification

import org.springframework.hateoas.RepresentationModel
import java.io.Serializable

data class NotificationSettingGroupModel(
  var inApp: Boolean,
  var email: Boolean,
) : RepresentationModel<NotificationSettingGroupModel>(),
  Serializable
