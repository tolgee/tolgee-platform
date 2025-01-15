package io.tolgee.service.notification

import io.tolgee.component.CurrentDateProvider
import io.tolgee.events.OnNotificationsChangedForUser
import io.tolgee.model.Notification
import io.tolgee.repository.NotificationRepository
import io.tolgee.websocket.*
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
  ): Page<Notification> {
    return notificationRepository.fetchNotificationsByUserId(userId, pageable)
  }

  fun save(notification: Notification) {
    notificationRepository.save(notification)
    applicationEventPublisher.publishEvent(OnNotificationsChangedForUser(notification.user.id))
  }

  @TransactionalEventListener
  fun onNotificationSaved(event: OnNotificationsChangedForUser) {
    websocketEventPublisher(
      "/users/${event.userId}/${WebsocketEventType.NOTIFICATIONS_CHANGED.typeName}",
      WebsocketEvent(
        timestamp = currentDateProvider.date.time,
      ),
    )
  }
}
