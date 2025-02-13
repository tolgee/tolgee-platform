package io.tolgee.testing

import io.tolgee.model.notifications.Notification
import io.tolgee.repository.notification.NotificationRepository
import org.springframework.data.domain.Sort
import org.springframework.data.domain.Sort.Direction.DESC
import org.springframework.stereotype.Component

@Component
class NotificationTestUtil(
  private val notificationRepository: NotificationRepository,
) {
  fun newestInAppNotification(): Notification = notificationRepository.findAll(Sort.by(DESC, "id")).first()
}
