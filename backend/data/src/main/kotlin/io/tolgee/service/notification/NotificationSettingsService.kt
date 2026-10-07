package io.tolgee.service.notification

import io.tolgee.model.UserAccount
import io.tolgee.model.notifications.Notification
import io.tolgee.model.notifications.NotificationChannel
import io.tolgee.model.notifications.NotificationSetting
import io.tolgee.model.notifications.NotificationType
import io.tolgee.model.notifications.NotificationTypeGroup
import io.tolgee.repository.notification.NotificationSettingRepository
import org.springframework.stereotype.Service

@Service
class NotificationSettingsService(
  private val notificationSettingRepository: NotificationSettingRepository,
) {
  fun getSettings(user: UserAccount): List<NotificationSetting> {
    val dbData = notificationSettingRepository.findByUserId(user.id)

    val groupSettings =
      NotificationTypeGroup.entries.filter { it != NotificationTypeGroup.LOCALIZATION }.flatMap { group ->
        NotificationChannel.entries.map { channel ->
          dbData.find { it.type == null && it.group == group && it.channel == channel }
            ?: newSetting(user, group, null, channel, true)
        }
      }

    val typeSettings =
      NotificationType.GROUPED.flatMap { type ->
        NotificationChannel.entries.map { channel ->
          dbData.find { it.type == type && it.channel == channel }
            ?: newSetting(user, type.group, type, channel, type.defaultFor(channel))
        }
      }

    return groupSettings + typeSettings
  }

  fun getSettingValue(
    notification: Notification,
    channel: NotificationChannel,
  ) = isEnabled(notification.user.id, notification.type, channel)

  fun isEnabled(
    userId: Long,
    type: NotificationType,
    channel: NotificationChannel,
  ): Boolean {
    if (type.group == NotificationTypeGroup.LOCALIZATION) {
      return notificationSettingRepository.findByUserIdAndTypeAndChannel(userId, type, channel)?.enabled
        ?: type.defaultFor(channel)
    }
    return notificationSettingRepository.findByUserIdAndGroupAndChannel(userId, type.group, channel)?.enabled
      ?: true
  }

  fun usersWithEnabled(
    userIds: Collection<Long>,
    type: NotificationType,
    channel: NotificationChannel,
  ): Set<Long> {
    if (userIds.isEmpty()) return emptySet()
    val stored =
      notificationSettingRepository
        .findByUserIdInAndTypeAndChannel(userIds, type, channel)
        .associate { it.user.id to it.enabled }
    return userIds.filter { stored[it] ?: type.defaultFor(channel) }.toSet()
  }

  fun save(
    user: UserAccount,
    group: NotificationTypeGroup,
    channel: NotificationChannel,
    enabled: Boolean,
  ) {
    val setting =
      notificationSettingRepository.findByUserIdAndGroupAndChannel(user.id, group, channel)
        ?: newSetting(user, group, null, channel, enabled)
    setting.enabled = enabled
    notificationSettingRepository.save(setting)
  }

  fun saveForType(
    user: UserAccount,
    type: NotificationType,
    channel: NotificationChannel,
    enabled: Boolean,
  ) {
    val setting =
      notificationSettingRepository.findByUserIdAndTypeAndChannel(user.id, type, channel)
        ?: newSetting(user, type.group, type, channel, enabled)
    setting.enabled = enabled
    notificationSettingRepository.save(setting)
  }

  private fun NotificationType.defaultFor(channel: NotificationChannel) =
    when (channel) {
      NotificationChannel.IN_APP -> defaultInApp
      NotificationChannel.EMAIL -> defaultEmail
    }

  private fun newSetting(
    user: UserAccount,
    group: NotificationTypeGroup,
    type: NotificationType?,
    channel: NotificationChannel,
    enabled: Boolean,
  ) = NotificationSetting().apply {
    this.user = user
    this.group = group
    this.type = type
    this.channel = channel
    this.enabled = enabled
  }
}
