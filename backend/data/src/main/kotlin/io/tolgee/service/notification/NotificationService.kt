package io.tolgee.service.notification

import io.tolgee.repository.NotificationRepository
import org.springframework.stereotype.Service

@Service
class NotificationService(
  private val notificationRepository: NotificationRepository,
) {
  fun getNotifications(userId: Long): List<NotificationModel> {
    return notificationRepository.fetchNotificationsByUserId(userId)
      .map { NotificationModel(it.id, it.linkedTask?.project?.id, it.linkedTask?.number, it.linkedTask?.name) }
  }
}
