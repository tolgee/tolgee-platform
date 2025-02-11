package io.tolgee.hateoas.notification

import io.tolgee.api.v2.controllers.notification.NotificationSettingsController
import io.tolgee.model.notifications.NotificationChannel
import io.tolgee.model.notifications.NotificationSetting
import io.tolgee.model.notifications.NotificationTypeGroup
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class NotificationSettingsModelAssembler :
  RepresentationModelAssemblerSupport<List<NotificationSetting>, NotificationSettingModel>(
    NotificationSettingsController::class.java,
    NotificationSettingModel::class.java,
  ) {
  override fun toModel(view: List<NotificationSetting>): NotificationSettingModel =
    NotificationSettingModel(
      NotificationTypeGroup.entries.associateWith { group ->
        NotificationChannel.entries.associateWith { channel ->
          view.find { it.group == group && it.channel == channel }?.enabled
            ?: throw IllegalStateException("Setting with group $group and channel $channel not found")
        }
      },
    )
}
