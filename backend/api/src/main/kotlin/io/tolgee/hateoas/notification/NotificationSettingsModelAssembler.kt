package io.tolgee.hateoas.notification

import io.tolgee.api.v2.controllers.notification.NotificationSettingsController
import io.tolgee.model.notifications.NotificationChannel
import io.tolgee.model.notifications.NotificationDigestFrequency
import io.tolgee.model.notifications.NotificationSetting
import io.tolgee.model.notifications.NotificationType
import io.tolgee.model.notifications.NotificationTypeGroup
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class NotificationSettingsModelAssembler :
  RepresentationModelAssemblerSupport<List<NotificationSetting>, NotificationSettingModel>(
    NotificationSettingsController::class.java,
    NotificationSettingModel::class.java,
  ) {
  fun toModel(
    view: List<NotificationSetting>,
    digestFrequency: NotificationDigestFrequency,
  ): NotificationSettingModel =
    NotificationSettingModel(
      accountSecurity = view.groupModel(NotificationTypeGroup.ACCOUNT_SECURITY),
      tasks = view.groupModel(NotificationTypeGroup.TASKS),
      localization =
        NotificationType.GROUPED.map { type ->
          NotificationTypeSettingModel(
            type = type,
            inApp = view.typeValue(type, NotificationChannel.IN_APP),
            email = view.typeValue(type, NotificationChannel.EMAIL),
          )
        },
      digestFrequency = digestFrequency,
    )

  override fun toModel(view: List<NotificationSetting>): NotificationSettingModel =
    toModel(view, NotificationDigestFrequency.DAILY)

  private fun List<NotificationSetting>.typeValue(
    type: NotificationType,
    channel: NotificationChannel,
  ) = find { it.type == type && it.channel == channel }?.enabled
    ?: throw IllegalStateException("Setting with type $type and channel $channel not found")

  private fun List<NotificationSetting>.groupModel(group: NotificationTypeGroup) =
    NotificationSettingGroupModel(
      inApp = findValue(group, NotificationChannel.IN_APP),
      email = findValue(group, NotificationChannel.EMAIL),
    )

  private fun List<NotificationSetting>.findValue(
    group: NotificationTypeGroup,
    channel: NotificationChannel,
  ) = (
    find { it.type == null && it.group == group && it.channel == channel }?.enabled
      ?: throw IllegalStateException("Setting with group $group and channel $channel not found")
  )
}
