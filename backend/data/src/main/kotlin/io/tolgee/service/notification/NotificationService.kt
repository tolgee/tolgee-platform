package io.tolgee.service.notification

import io.tolgee.model.Notification
import io.tolgee.repository.NotificationRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Service
class NotificationService(
  private val notificationRepository: NotificationRepository,
) {
  fun getNotifications(
    userId: Long,
    pageable: Pageable,
  ): Page<Notification> {
    return notificationRepository.fetchNotificationsByUserId(userId, pageable)
  }

  fun save(notification: Notification) {
    notificationRepository.save(notification)
  }
}
