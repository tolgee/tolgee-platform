package io.tolgee.service

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

data class NotificationModel(
  val id: Long,
  val linkedProjectId: Long?,
  val linkedTaskNumber: Long?,
  val linkedTaskName: String?,
)
