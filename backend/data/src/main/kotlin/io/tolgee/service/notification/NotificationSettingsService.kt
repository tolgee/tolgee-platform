package io.tolgee.service.notification

import io.tolgee.model.UserAccount
import io.tolgee.model.notifications.Notification
import io.tolgee.model.notifications.NotificationChannel
import io.tolgee.model.notifications.NotificationSetting
import io.tolgee.model.notifications.NotificationTypeGroup
import io.tolgee.repository.notification.NotificationSettingRepository
import org.springframework.stereotype.Service

@Service
class NotificationSettingsService(
  private val notificationSettingRepository: NotificationSettingRepository,
) {
  fun getSettings(user: UserAccount): List<NotificationSetting> {
    val dbData = notificationSettingRepository.findByUserId(user.id)

    return NotificationTypeGroup.entries.flatMap { group ->
      NotificationChannel.entries.map { channel ->
        dbData.find { it.group == group && it.channel == channel } ?: getDefaultSettings(user, group, channel)
      }
    }
  }

  fun getSettingValue(
    notification: Notification,
    channel: NotificationChannel,
  ) = notificationSettingRepository
    .findByUserIdAndGroupAndChannel(
      notification.user.id,
      notification.type.group,
      channel,
    )?.enabled ?: true

  private fun getDefaultSettings(
    user: UserAccount,
    group: NotificationTypeGroup,
    channel: NotificationChannel,
  ) = NotificationSetting().apply {
    this.user = user
    this.group = group
    this.channel = channel
    this.enabled = true
  }

  fun save(
    user: UserAccount,
    group: NotificationTypeGroup,
    channel: NotificationChannel,
    enabled: Boolean,
  ) {
    val setting =
      notificationSettingRepository.findByUserIdAndGroupAndChannel(user.id, group, channel)
        ?: NotificationSetting().apply {
          this.user = user
          this.group = group
          this.channel = channel
        }
    setting.enabled = enabled
    notificationSettingRepository.save(setting)
  }
}
