package io.tolgee.repository.notification

import io.tolgee.model.notifications.NotificationDigestState
import org.springframework.context.annotation.Lazy
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
@Lazy
interface NotificationDigestStateRepository : JpaRepository<NotificationDigestState, Long> {
  @Modifying
  @Query("UPDATE NotificationDigestState s SET s.lastDigestSentAt = null WHERE s.userId = :userId")
  fun resetWindow(userId: Long): Int
}
