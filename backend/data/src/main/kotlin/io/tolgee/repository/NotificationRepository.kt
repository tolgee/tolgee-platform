package io.tolgee.repository

import io.tolgee.dtos.request.notification.NotificationFilters
import io.tolgee.model.Notification
import org.springframework.context.annotation.Lazy
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

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
        AND (
                :#{#filters.filterSeen} is null
                OR :#{#filters.filterSeen} = n.seen
            )
    """,
  )
  fun fetchNotificationsByUserId(
    userId: Long,
    pageable: Pageable,
    filters: NotificationFilters,
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
