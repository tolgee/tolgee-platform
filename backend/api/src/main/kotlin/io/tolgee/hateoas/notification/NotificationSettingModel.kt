package io.tolgee.hateoas.notification

import org.springframework.hateoas.RepresentationModel
import java.io.Serializable

data class NotificationSettingModel(
  var accountSecurity: NotificationSettingGroupModel,
  var tasks: NotificationSettingGroupModel,
  var keysAdded: NotificationSettingGroupModel,
  var stringsTranslated: NotificationSettingGroupModel,
  var stringsReviewed: NotificationSettingGroupModel,
) : RepresentationModel<NotificationSettingModel>(),
  Serializable
