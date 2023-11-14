/**
 * Copyright (C) 2023 Tolgee s.r.o. and contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.tolgee.notifications

import io.tolgee.model.Notification
import io.tolgee.model.UserAccount
import io.tolgee.repository.NotificationsRepository
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.Pageable
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.EnumSet

@Service
class NotificationService(
  private val notificationsRepository: NotificationsRepository,
  private val applicationEventPublisher: ApplicationEventPublisher,
) {
  @Async
  @Transactional
  fun dispatchNotification(notification: Notification, user: UserAccount) =
    dispatchNotification(notification, setOf(user))

  @Async
  @Transactional
  fun dispatchNotification(notification: Notification, users: Set<UserAccount>) {
    val localUsers = users.toMutableSet()
    val notificationObjects = mutableSetOf<Notification>()

    if (debouncedNotificationTypes.contains(notification.type)) {
      val existingNotifications = notificationsRepository.findAllByUnreadTrueAndTypeAndProjectAndRecipientIn(
        notification.type,
        notification.project,
        users,
      )

      existingNotifications.forEach {
        localUsers.remove(it.recipient)

        when (notification.type) {
          Notification.NotificationType.ACTIVITY ->
            it.activityRevisions!!.addAll(notification.activityRevisions!!)
          else ->
            throw RuntimeException("Cannot debounce notification type ${notification.type}")
        }
      }

      // Push modifications
      notificationObjects.addAll(
        notificationsRepository.saveAll(existingNotifications)
      )
    }

    // Create notifications
    localUsers.forEach {
      notification.recipient = it
      notificationObjects.add(
        notificationsRepository.save(notification)
      )
    }

    // Dispatch event
    applicationEventPublisher.publishEvent(
      NotificationPushEvent(notificationObjects)
    )
  }

  fun getNotificationsNotDone(user: UserAccount, pageable: Pageable): Collection<Notification> {
    return notificationsRepository.findAllByMarkedDoneAtNullAndRecipient(user, pageable)
  }

  fun getNotificationsDone(user: UserAccount, pageable: Pageable): Collection<Notification> {
    return notificationsRepository.findAllByMarkedDoneAtNotNullAndRecipient(user, pageable)
  }

  fun getUnreadNotificationsCount(user: UserAccount): Int {
    return notificationsRepository.countNotificationsByRecipientAndUnreadTrue(user)
  }

  fun markAsRead(user: UserAccount, notifications: Set<Long>) {
    return notificationsRepository.markAllAsRead(user)
  }

  fun markAllAsRead(user: UserAccount) {
    return notificationsRepository.markAllAsRead(user)
  }

  fun markAsDone(user: UserAccount, notifications: Set<Long>) {
    return notificationsRepository.markAllAsDone(user)
  }

  fun markAllAsDone(user: UserAccount) {
    return notificationsRepository.markAllAsDone(user)
  }

  companion object {
    val debouncedNotificationTypes: EnumSet<Notification.NotificationType>
      = EnumSet.of(Notification.NotificationType.ACTIVITY)
  }
}
