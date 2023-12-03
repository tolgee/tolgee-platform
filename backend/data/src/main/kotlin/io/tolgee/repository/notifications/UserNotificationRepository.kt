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

package io.tolgee.repository.notifications

import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.model.notifications.UserNotification
import io.tolgee.notifications.NotificationType
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface UserNotificationRepository : JpaRepository<UserNotification, Long> {
  fun findAllByRecipient(recipient: UserAccount): List<UserNotification>

  fun findAllByMarkedDoneAtNullAndRecipient(recipient: UserAccount, pageable: Pageable): List<UserNotification>

  fun findAllByMarkedDoneAtNotNullAndRecipient(recipient: UserAccount, pageable: Pageable): List<UserNotification>

  fun countNotificationsByRecipientAndUnreadTrue(recipient: UserAccount): Int

  @Query(
    """
      FROM UserNotification un
      INNER JOIN un.modifiedEntities me
      INNER JOIN ActivityDescribingEntity de ON de.activityRevision = me.activityRevision
      WHERE
        un.type = :type AND
        un.unread = true AND
        un.project = :project AND
        un.recipient IN :recipients
    """
  )
  fun findCandidatesForNotificationDebouncing(
    type: NotificationType,
    project: Project,
    recipients: Collection<UserAccount>,
  ): List<UserNotification>

  @Query(
    """
      SELECT un
      FROM UserNotification un
      INNER JOIN un.modifiedEntities me
      WHERE
        un.unread = true AND
        un.project = :project AND
        un.recipient IN :recipients AND (
          un.type = :type OR (
            un.type = io.tolgee.notifications.NotificationType.ACTIVITY_KEYS_CREATED AND
            me.entityClass = 'Key' AND
            me.entityId = :keyId
          )
        )
      ORDER BY un.type DESC
    """
  )
  fun findCandidatesForTranslationUpdateNotificationDebouncing(
    type: NotificationType,
    project: Project,
    recipients: Collection<UserAccount>,
    keyId: Long,
  ): List<UserNotification>

  @Query(
    """
      SELECT un
      FROM UserNotification un
      INNER JOIN un.modifiedEntities me
      INNER JOIN ActivityDescribingEntity de ON de.activityRevision = me.activityRevision
      WHERE
        un.unread = true AND
        me.entityClass = 'TranslationComment' AND
        de.entityClass = 'Translation' AND
        de.entityId = :translationId AND
        un.project = :project AND
        un.recipient IN :recipients AND
        un.type IN (
          io.tolgee.notifications.NotificationType.ACTIVITY_NEW_COMMENTS,
          io.tolgee.notifications.NotificationType.ACTIVITY_COMMENTS_MENTION
        )
    """
  )
  fun findCandidatesForCommentNotificationDebouncing(
    project: Project,
    recipients: Collection<UserAccount>,
    translationId: Long,
  ): List<UserNotification>

  @Modifying
  @Query("UPDATE UserNotification un SET un.unread = false WHERE un.recipient = ?1 AND un.id IN ?2")
  fun markAsRead(recipient: UserAccount, notifications: Collection<Long>)

  @Modifying
  @Query("UPDATE UserNotification un SET un.unread = false WHERE un.recipient = ?1")
  fun markAllAsRead(recipient: UserAccount)

  @Modifying
  @Query("UPDATE UserNotification un SET un.unread = true WHERE un.recipient = ?1 AND un.id IN ?2")
  fun markAsUnread(recipient: UserAccount, notifications: Collection<Long>)

  @Modifying
  @Query("UPDATE UserNotification un SET un.unread = true WHERE un.recipient = ?1")
  fun markAllAsUnread(recipient: UserAccount)

  @Modifying
  @Query(
    "UPDATE UserNotification un SET un.unread = false, un.markedDoneAt = NOW() WHERE un.recipient = ?1 AND un.id IN ?2"
  )
  fun markAsDone(recipient: UserAccount, notifications: Collection<Long>)

  @Modifying
  @Query("UPDATE UserNotification un SET un.unread = false, un.markedDoneAt = NOW() WHERE un.recipient = ?1")
  fun markAllAsDone(recipient: UserAccount)

  @Modifying
  @Query("UPDATE UserNotification un SET un.markedDoneAt = null WHERE un.recipient = ?1 AND un.id IN ?2")
  fun unmarkAsDone(recipient: UserAccount, notifications: Collection<Long>)

  @Modifying
  @Query("DELETE FROM user_notification WHERE marked_done_at < NOW() - INTERVAL '90 DAY'", nativeQuery = true)
  fun pruneOldNotifications() // Native query since HQL can't do "INTERVAL"

  @Modifying
  @Query("DELETE FROM UserNotification WHERE recipient.id = :userId")
  fun deleteAllByUserId(userId: Long)

  @Modifying
  @Query("DELETE FROM UserNotification WHERE project.id = :projectId")
  fun deleteAllByProjectId(projectId: Long)
}
