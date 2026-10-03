package io.tolgee.repository.notification

import io.tolgee.model.notifications.NotificationDigestState
import org.springframework.context.annotation.Lazy
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.Date

@Repository
@Lazy
interface NotificationDigestStateRepository : JpaRepository<NotificationDigestState, Long> {
  @Modifying
  @Query("UPDATE NotificationDigestState s SET s.lastDigestSentAt = null WHERE s.userId = :userId")
  fun resetWindow(userId: Long): Int

  @Modifying
  @Query(
    """
    insert into notification_digest_state (user_id, frequency, last_digest_sent_at)
    values (:userId, 'DAILY', :sentAt)
    on conflict (user_id) do update set last_digest_sent_at = excluded.last_digest_sent_at
    """,
    nativeQuery = true,
  )
  fun upsertLastDigestSentAt(
    userId: Long,
    sentAt: Date,
  ): Int

  @Modifying
  @Query(
    """
    insert into notification_digest_state (user_id, frequency, last_digest_sent_at)
    values (:userId, :frequency, null)
    on conflict (user_id) do update set frequency = excluded.frequency
    """,
    nativeQuery = true,
  )
  fun upsertFrequency(
    userId: Long,
    frequency: String,
  ): Int
}
