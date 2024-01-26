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

import io.tolgee.model.notifications.UserNotification
import io.tolgee.notifications.dto.NotificationCreateDto
import io.tolgee.notifications.dto.UserNotificationParamsDto
import io.tolgee.notifications.events.UserNotificationPushEvent
import io.tolgee.repository.notifications.UserNotificationRepository
import jakarta.persistence.EntityManager
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserNotificationService(
	private val entityManager: EntityManager,
  private val userNotificationRepository: UserNotificationRepository,
  private val applicationEventPublisher: ApplicationEventPublisher,
  private val userNotificationDebouncer: UserNotificationDebouncer,
) {
  @Transactional
  fun dispatchNotification(
    notificationDto: NotificationCreateDto,
    params: UserNotificationParamsDto,
  ) {
    return dispatchNotifications(notificationDto, listOf(params))
  }

  @Transactional
  fun dispatchNotifications(
    notificationDto: NotificationCreateDto,
    params: List<UserNotificationParamsDto>,
  ) {
    val createdUserNotificationObjects = mutableSetOf<UserNotification>()
    val updatedUserNotificationObjects = mutableSetOf<UserNotification>()

    val (processed, remaining) = userNotificationDebouncer.debounce(notificationDto, params)
    updatedUserNotificationObjects.addAll(
      userNotificationRepository.saveAll(processed),
    )

    remaining.forEach {
      val notification = notificationDto.toUserNotificationEntity(it, entityManager)
      createdUserNotificationObjects.add(
        userNotificationRepository.save(notification),
      )
    }

    // Dispatch event
    applicationEventPublisher.publishEvent(
      UserNotificationPushEvent(
        createdUserNotificationObjects,
        updatedUserNotificationObjects,
      ),
    )
  }

  fun findNotificationsOfUserFilteredPaged(
    user: Long,
    status: Set<NotificationStatus>,
    pageable: Pageable,
  ): List<UserNotification> {
    return userNotificationRepository.findNotificationsOfUserFilteredPaged(
      user,
      status.map { it.toString() },
      pageable,
    )
  }

  fun getUnreadNotificationsCount(user: Long): Int {
    return userNotificationRepository.countNotificationsByRecipientIdAndUnreadTrue(user)
  }

  @Transactional
  fun markAsRead(
    user: Long,
    notifications: Collection<Long>,
  ) {
    return userNotificationRepository.markAsRead(user, notifications)
  }

  @Transactional
  fun markAllAsRead(user: Long) {
    return userNotificationRepository.markAllAsRead(user)
  }

  @Transactional
  fun markAsUnread(
    user: Long,
    notifications: Collection<Long>,
  ) {
    return userNotificationRepository.markAsUnread(user, notifications)
  }

  @Transactional
  fun markAsDone(
    user: Long,
    notifications: Collection<Long>,
  ) {
    return userNotificationRepository.markAsDone(user, notifications)
  }

  @Transactional
  fun markAllAsDone(user: Long) {
    return userNotificationRepository.markAllAsDone(user)
  }

  @Transactional
  fun unmarkAsDone(
    user: Long,
    notifications: Collection<Long>,
  ) {
    return userNotificationRepository.unmarkAsDone(user, notifications)
  }

  @Transactional
  fun deleteAllByUserId(userId: Long) {
    userNotificationRepository.deleteAllByUserId(userId)
  }

  @Transactional
  fun deleteAllByProjectId(projectId: Long) {
    userNotificationRepository.deleteAllByProjectId(projectId)
  }
}
