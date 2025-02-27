package io.tolgee.service.notification

import io.tolgee.dtos.request.notification.NotificationFilters
import io.tolgee.dtos.response.CursorValue
import io.tolgee.events.OnNotificationsChangedForUser
import io.tolgee.model.notifications.Notification
import io.tolgee.model.notifications.NotificationChannel
import io.tolgee.model.notifications.NotificationTypeGroup
import io.tolgee.repository.notification.NotificationRepository
import jakarta.transaction.Transactional
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import java.sql.Timestamp
import java.time.Instant

@Service
class NotificationService(
  private val notificationRepository: NotificationRepository,
  private val applicationEventPublisher: ApplicationEventPublisher,
  private val emailNotificationsService: EmailNotificationsService,
  private val notificationSettingsService: NotificationSettingsService,
) {
  fun getNotifications(
    userId: Long,
    pageable: Pageable = Pageable.unpaged(),
    filters: NotificationFilters = NotificationFilters(),
    cursor: Map<String, CursorValue>? = null,
  ): Page<Notification> =
    notificationRepository.fetchNotificationsByUserId(
      userId,
      pageable,
      filters,
      cursor?.get("createdAt")?.value?.let { Timestamp.from(Instant.ofEpochMilli(it.toLong())) },
      cursor?.get("id")?.value?.toLong(),
    )

  fun getCountOfUnseenNotifications(userId: Long): Int =
    notificationRepository
      .fetchNotificationsByUserId(
        userId,
        PageRequest.of(0, 1),
        NotificationFilters(false),
      ).totalElements
      .toInt()

  fun deleteNotificationsOfUser(userId: Long) {
    getNotifications(userId).forEach {
      notificationRepository.delete(it)
    }
  }

  @Transactional
  fun notify(notification: Notification) {
    if (notification.type.group == NotificationTypeGroup.TASKS && notification.linkedTask == null) {
      throw IllegalArgumentException("Task notification must have a linked task")
    }
    if (notificationSettingsService.getSettingValue(notification, NotificationChannel.EMAIL)) {
      emailNotificationsService.sendEmailNotification(notification)
    }
    if (notificationSettingsService.getSettingValue(notification, NotificationChannel.IN_APP)) {
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
