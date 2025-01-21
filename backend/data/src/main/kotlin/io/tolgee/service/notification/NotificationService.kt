package io.tolgee.service.notification

import io.tolgee.component.CurrentDateProvider
import io.tolgee.dtos.request.notification.NotificationFilters
import io.tolgee.events.OnNotificationsChangedForUser
import io.tolgee.model.Notification
import io.tolgee.repository.NotificationRepository
import io.tolgee.websocket.WebsocketEvent
import io.tolgee.websocket.WebsocketEventPublisher
import io.tolgee.websocket.WebsocketEventType
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.event.TransactionalEventListener

@Service
class NotificationService(
  private val notificationRepository: NotificationRepository,
  private val websocketEventPublisher: WebsocketEventPublisher,
  private val currentDateProvider: CurrentDateProvider,
  private val applicationEventPublisher: ApplicationEventPublisher,
) {
  fun getNotifications(
    userId: Long,
    pageable: Pageable,
    filters: NotificationFilters,
  ): Page<Notification> = notificationRepository.fetchNotificationsByUserId(userId, pageable, filters)

  fun save(notification: Notification) {
    notificationRepository.save(notification)
    applicationEventPublisher.publishEvent(OnNotificationsChangedForUser(notification.user.id))
  }

  fun markNotificationsAsSeen(
    notificationIds: List<Long>,
    userId: Long,
  ) {
    val modifiedCount = notificationRepository.markNotificationsAsSeen(notificationIds, userId)

    if (modifiedCount > 0) {
      sendWebsocketEvent(userId)
    }
  }

  @TransactionalEventListener
  fun onNotificationSaved(event: OnNotificationsChangedForUser) {
    sendWebsocketEvent(event.userId)
  }

  private fun sendWebsocketEvent(userId: Long) {
    websocketEventPublisher(
      "/users/$userId/${WebsocketEventType.NOTIFICATIONS_CHANGED.typeName}",
      WebsocketEvent(
        timestamp = currentDateProvider.date.time,
      ),
    )
  }
}
