package io.tolgee.service.notification

import io.tolgee.dtos.request.notification.NotificationFilters
import io.tolgee.events.OnNotificationsChangedForUser
import io.tolgee.model.notifications.Notification
import io.tolgee.model.notifications.NotificationChannel
import io.tolgee.repository.notification.NotificationRepository
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
  private val notificationSettingService: NotificationSettingService,
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
  fun notify(notification: Notification) {
    if (notificationSettingService.getSettingValue(notification, NotificationChannel.EMAIL)) {
      emailNotificationsService.sendEmailNotification(notification)
    }
    if (notificationSettingService.getSettingValue(notification, NotificationChannel.IN_APP)) {
      notificationRepository.save(notification)
      applicationEventPublisher.publishEvent(
        OnNotificationsChangedForUser(
          notification.user.id,
          notification,
        ),
      )
    }
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
