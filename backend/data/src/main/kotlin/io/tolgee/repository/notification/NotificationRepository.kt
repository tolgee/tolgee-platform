package io.tolgee.repository.notification

import io.tolgee.dtos.request.notification.NotificationFilters
import io.tolgee.model.notifications.Notification
import org.springframework.context.annotation.Lazy
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.sql.Timestamp
import java.util.Date

@Repository
@Lazy
interface NotificationRepository : JpaRepository<Notification, Long> {
  @Query(
    """
    SELECT n
     FROM Notification n
     LEFT JOIN FETCH n.user AS u
     LEFT JOIN FETCH n.originatingUser
     LEFT JOIN FETCH n.linkedTask
     WHERE u.id = :userId
        AND n.emailOnly = false
        AND u.deletedAt is null
        AND u.disabledAt is null
        AND (
                :#{#filters.filterSeen} is null
                OR :#{#filters.filterSeen} = n.seen
            )
        AND (
                CAST(:cursorUpdatedAt AS timestamp) is null
                OR :cursorId is null
                OR :cursorUpdatedAt > n.updatedAt
                OR :cursorUpdatedAt = n.updatedAt AND :cursorId > n.id
            )
    """,
  )
  fun fetchNotificationsByUserId(
    userId: Long,
    pageable: Pageable,
    filters: NotificationFilters,
    cursorUpdatedAt: Timestamp? = null,
    cursorId: Long? = null,
  ): Page<Notification>

  @Query(
    """
    SELECT n
     FROM Notification n
     LEFT JOIN FETCH n.originatingUser
     LEFT JOIN FETCH n.linkedTask
     WHERE n.id = :id
        AND n.user.id = :userId
        AND n.emailOnly = false
    """,
  )
  fun findVisibleToUser(
    id: Long,
    userId: Long,
  ): Notification?

  @Modifying
  @Query("DELETE FROM Notification n WHERE n.user.id = :userId")
  fun deleteAllOfUser(userId: Long)

  @Modifying
  @Query(
    """
    DELETE FROM Notification n
    WHERE n.linkedComment.id = :commentId
        AND n.type = io.tolgee.model.notifications.NotificationType.COMMENT_MENTION
        AND n.seen = false
        AND n.user.id NOT IN :keepUserIds
    """,
  )
  fun deleteUnseenMentionsOfCommentExcept(
    commentId: Long,
    keepUserIds: Collection<Long>,
  )

  @Query(
    """
    UPDATE Notification n
    SET n.seen = true, n.seenAt = :seenAt
    WHERE n.user.id = :userId
        AND n.id IN :notificationIds 
        AND n.seen = false
    """,
  )
  @Modifying
  fun markNotificationsAsSeen(
    notificationIds: List<Long>,
    userId: Long,
    seenAt: Date,
  ): Int
}
