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
import io.tolgee.model.translation.TranslationComment
import io.tolgee.notifications.dto.NotificationCreateDto
import io.tolgee.notifications.dto.UserNotificationParamsDto
import io.tolgee.repository.UserNotificationRepository
import org.springframework.stereotype.Component
import java.util.*

typealias UserNotificationDebounceResult = Pair<List<UserNotification>, List<UserNotificationParamsDto>>

/**
 * Component responsible for debouncing notifications based on existing notifications for a given user.
 */
@Component
class UserNotificationDebouncer(
  private val userNotificationRepository: UserNotificationRepository
) {
  /**
   * Updates existing notifications when possible according to the debouncing policy.
   *
   * @return A pair of the notifications which have been updated, and the remaining notifications to process.
   */
  fun debounce(
    notificationDto: NotificationCreateDto,
    params: List<UserNotificationParamsDto>
  ): UserNotificationDebounceResult {
    if (NotificationType.ACTIVITY_NOTIFICATIONS.contains(notificationDto.type)) {
      return debounceActivityNotification(notificationDto, params)
    }

    return Pair(emptyList(), params)
  }

  // --
  // Activity-related notifications
  // --
  private fun debounceActivityNotification(
    notificationDto: NotificationCreateDto,
    params: List<UserNotificationParamsDto>,
  ): UserNotificationDebounceResult {
    val debouncedUserNotifications = mutableListOf<UserNotification>()
    val notificationsToProcess = mutableListOf<UserNotificationParamsDto>()
    val notifications = fetchRelevantActivityNotifications(notificationDto, params.map { it.recipient })

    params.forEach {
      if (notifications.containsKey(it.recipient.id)) {
        val notification = notifications[it.recipient.id]!!
        notificationDto.mergeIntoUserNotificationEntity(notification, it)
        debouncedUserNotifications.add(notification)
      } else {
        notificationsToProcess.add(it)
      }
    }

    return Pair(debouncedUserNotifications, notificationsToProcess)
  }

  private fun fetchRelevantActivityNotifications(
    notificationDto: NotificationCreateDto,
    recipients: List<UserAccount>,
  ): Map<Long, UserNotification> {
    if (commentNotificationTypes.contains(notificationDto.type))
      return fetchRelevantCommentActivityNotifications(notificationDto, recipients)

    val notifications = userNotificationRepository.findCandidatesForNotificationDebouncing(
      notificationDto.type,
      notificationDto.project,
      recipients,
    )

    return notifications.associateBy { it.recipient.id }
  }

  private fun fetchRelevantCommentActivityNotifications(
    notificationDto: NotificationCreateDto,
    recipients: List<UserAccount>,
  ): Map<Long, UserNotification> {
    val translationId = notificationDto.modifiedEntities
      ?.find { it.entityClass == TranslationComment::class.simpleName }
      ?.describingRelations?.get("translation")?.entityId
      ?: return emptyMap()

    val notifications = userNotificationRepository.findCandidatesForCommentNotificationDebouncing(
      notificationDto.project,
      recipients,
      translationId,
    )

    return notifications.associateBy { it.recipient.id }
  }

  companion object {
    val commentNotificationTypes: EnumSet<NotificationType> = EnumSet.of(
      NotificationType.ACTIVITY_NEW_COMMENTS,
      NotificationType.ACTIVITY_COMMENTS_MENTION,
    )
  }
}
