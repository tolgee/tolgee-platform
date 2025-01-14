package io.tolgee.repository

import io.tolgee.model.Notification
import org.springframework.context.annotation.Lazy
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
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
     ORDER BY n.id DESC
    """,
  )
  fun fetchNotificationsByUserId(
    userId: Long,
    pageable: Pageable,
  ): Page<Notification>
}
