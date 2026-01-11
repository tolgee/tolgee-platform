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
        AND u.deletedAt is null
        AND u.disabledAt is null
        AND (
                :#{#filters.filterSeen} is null
                OR :#{#filters.filterSeen} = n.seen
            )
        AND (
                CAST(:cursorCreatedAt AS timestamp) is null
                OR :cursorId is null
                OR :cursorCreatedAt > n.createdAt
                OR :cursorCreatedAt = n.createdAt AND :cursorId > n.id
            )
    """,
  )
  fun fetchNotificationsByUserId(
    userId: Long,
    pageable: Pageable,
    filters: NotificationFilters,
    cursorCreatedAt: Timestamp? = null,
    cursorId: Long? = null,
  ): Page<Notification>

  @Query(
    """
    UPDATE Notification n
    SET n.seen = true
    WHERE n.user.id = :userId
        AND n.id IN :notificationIds 
        AND n.seen = false
    """,
  )
  @Modifying
  fun markNotificationsAsSeen(
    notificationIds: List<Long>,
    userId: Long,
  ): Int
}
