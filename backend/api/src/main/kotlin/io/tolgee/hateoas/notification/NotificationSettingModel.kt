package io.tolgee.hateoas.notification

import org.springframework.hateoas.RepresentationModel
import java.io.Serializable

data class NotificationSettingModel(
  var items: List<NotificationSettingGroupModel>
) : RepresentationModel<NotificationSettingModel>(), Serializable
