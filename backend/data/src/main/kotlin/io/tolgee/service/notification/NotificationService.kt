package io.tolgee.service.notification

import io.tolgee.dtos.request.notification.NotificationFilters
import io.tolgee.events.OnNotificationsChangedForUser
import io.tolgee.model.Notification
import io.tolgee.repository.NotificationRepository
import jakarta.transaction.Transactional
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Service
class NotificationService(
  private val notificationRepository: NotificationRepository,
  private val applicationEventPublisher: ApplicationEventPublisher,
  private val emailNotificationsService: EmailNotificationsService,
) {
  fun getNotifications(
    userId: Long,
    pageable: Pageable,
    filters: NotificationFilters,
  ): Page<Notification> = notificationRepository.fetchNotificationsByUserId(userId, pageable, filters)

  fun getCountOfUnseenNotifications(userId: Long): Int =
    notificationRepository
      .fetchNotificationsByUserId(
        userId,
        PageRequest.of(0, 1),
        NotificationFilters(false),
      ).totalElements
      .toInt()

  @Transactional
  fun save(notification: Notification) {
    emailNotificationsService.sendEmailNotification(notification)
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
