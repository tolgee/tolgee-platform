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

import io.tolgee.model.UserAccount
import io.tolgee.model.UserNotification
import io.tolgee.notifications.dto.NotificationCreateDto
import io.tolgee.notifications.dto.UserNotificationParamsDto
import io.tolgee.notifications.events.UserNotificationPushEvent
import io.tolgee.repository.UserNotificationRepository
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserNotificationService(
  private val userNotificationRepository: UserNotificationRepository,
  private val applicationEventPublisher: ApplicationEventPublisher,
  private val userNotificationDebouncer: UserNotificationDebouncer,
) {
  @Transactional
  fun dispatchNotification(notificationDto: NotificationCreateDto, params: UserNotificationParamsDto) {
    return dispatchNotifications(notificationDto, listOf(params))
  }

  @Transactional
  fun dispatchNotifications(notificationDto: NotificationCreateDto, params: List<UserNotificationParamsDto>) {
    val userNotificationObjects = mutableSetOf<UserNotification>()

    val (processed, remaining) = userNotificationDebouncer.debounce(notificationDto, params)
    userNotificationObjects.addAll(
      userNotificationRepository.saveAll(processed)
    )

    remaining.forEach {
      val notification = notificationDto.toUserNotificationEntity(it)
      userNotificationObjects.add(
        userNotificationRepository.save(notification)
      )
    }

    // Dispatch event
    applicationEventPublisher.publishEvent(
      UserNotificationPushEvent(userNotificationObjects)
    )
  }

  fun getNotificationsNotDone(user: UserAccount, pageable: Pageable): Collection<UserNotification> {
    return userNotificationRepository.findAllByMarkedDoneAtNullAndRecipient(user, pageable)
  }

  fun getNotificationsDone(user: UserAccount, pageable: Pageable): Collection<UserNotification> {
    return userNotificationRepository.findAllByMarkedDoneAtNotNullAndRecipient(user, pageable)
  }

  fun getUnreadNotificationsCount(user: UserAccount): Int {
    return userNotificationRepository.countNotificationsByRecipientAndUnreadTrue(user)
  }

  fun markAsRead(user: UserAccount, notifications: Set<Long>) {
    return userNotificationRepository.markAllAsRead(user)
  }

  fun markAllAsRead(user: UserAccount) {
    return userNotificationRepository.markAllAsRead(user)
  }

  fun markAsDone(user: UserAccount, notifications: Set<Long>) {
    return userNotificationRepository.markAllAsDone(user)
  }

  fun markAllAsDone(user: UserAccount) {
    return userNotificationRepository.markAllAsDone(user)
  }

  fun deleteAllByUserId(userId: Long) {
    userNotificationRepository.deleteAllByUserId(userId)
  }

  fun deleteAllByProjectId(projectId: Long) {
    userNotificationRepository.deleteAllByProjectId(projectId)
  }
}
