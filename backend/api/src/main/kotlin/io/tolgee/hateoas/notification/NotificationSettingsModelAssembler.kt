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
      accountSecurity = view.groupModel(NotificationTypeGroup.ACCOUNT_SECURITY),
      tasks = view.groupModel(NotificationTypeGroup.TASKS),
    )

  private fun List<NotificationSetting>.groupModel(group: NotificationTypeGroup) =
    NotificationSettingGroupModel(
      inApp = findValue(group, NotificationChannel.IN_APP),
      email = findValue(group, NotificationChannel.EMAIL),
    )

  private fun List<NotificationSetting>.findValue(
    group: NotificationTypeGroup,
    channel: NotificationChannel,
  ) = (
    find { it.group == group && it.channel == channel }?.enabled
      ?: throw IllegalStateException("Setting with group $group and channel $channel not found")
  )
}
