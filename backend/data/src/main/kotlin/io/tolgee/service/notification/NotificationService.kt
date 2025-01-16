package io.tolgee.service.notification

import io.tolgee.events.OnNotificationsChangedForUser
import io.tolgee.model.Notification
import io.tolgee.repository.NotificationRepository
import jakarta.transaction.Transactional
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Service
class NotificationService(
  private val notificationRepository: NotificationRepository,
  private val applicationEventPublisher: ApplicationEventPublisher,
) {
  fun getNotifications(
    userId: Long,
    pageable: Pageable,
  ): Page<Notification> {
    return notificationRepository.fetchNotificationsByUserId(userId, pageable)
  }

  fun getCountOfUnseenNotifications(userId: Long): Int {
    return notificationRepository.getUnseenCountByUserId(userId)
  }

  @Transactional
  fun save(notification: Notification) {
    notificationRepository.save(notification)
    applicationEventPublisher.publishEvent(
      OnNotificationsChangedForUser(
        notification.user.id,
        notification,
      ),
    )
  }

  @Transactional
  fun markNotificationsAsSeen(
    notificationIds: List<Long>,
    userId: Long,
  ) {
    val modifiedCount = notificationRepository.markNotificationsAsSeen(notificationIds, userId)

    if (modifiedCount > 0) {
      applicationEventPublisher.publishEvent(
        OnNotificationsChangedForUser(
          userId,
        ),
      )
    }
  }
}
