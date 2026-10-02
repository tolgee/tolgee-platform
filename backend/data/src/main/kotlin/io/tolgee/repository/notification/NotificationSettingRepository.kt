package io.tolgee.repository.notification

import io.tolgee.model.notifications.NotificationChannel
import io.tolgee.model.notifications.NotificationSetting
import io.tolgee.model.notifications.NotificationType
import io.tolgee.model.notifications.NotificationTypeGroup
import org.springframework.context.annotation.Lazy
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
@Lazy
interface NotificationSettingRepository : JpaRepository<NotificationSetting, Long> {
  fun findByUserId(userId: Long): List<NotificationSetting>

  @Query(
    """
    SELECT s FROM NotificationSetting s
    WHERE s.user.id = :userId AND s.group = :group AND s.channel = :channel AND s.type IS NULL
    """,
  )
  fun findByUserIdAndGroupAndChannel(
    userId: Long,
    group: NotificationTypeGroup,
    channel: NotificationChannel,
  ): NotificationSetting?

  fun findByUserIdAndTypeAndChannel(
    userId: Long,
    type: NotificationType,
    channel: NotificationChannel,
  ): NotificationSetting?

  fun findByUserIdInAndTypeAndChannel(
    userIds: Collection<Long>,
    type: NotificationType,
    channel: NotificationChannel,
  ): List<NotificationSetting>
}
