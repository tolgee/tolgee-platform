package io.tolgee.repository.notification

import io.tolgee.model.notifications.NotificationSetting
import org.springframework.context.annotation.Lazy
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
@Lazy
interface NotificationSettingRepository : JpaRepository<NotificationSetting, Long> {
  fun findByUserId(userId: Long): List<NotificationSetting>
}
