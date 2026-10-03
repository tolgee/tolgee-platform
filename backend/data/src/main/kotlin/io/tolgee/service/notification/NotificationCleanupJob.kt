package io.tolgee.service.notification

import io.tolgee.component.CurrentDateProvider
import io.tolgee.component.LockingProvider
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.util.Logging
import io.tolgee.util.logger
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.sql.Timestamp
import java.time.Duration

@Component
class NotificationCleanupJob(
  private val jdbcTemplate: JdbcTemplate,
  private val lockingProvider: LockingProvider,
  private val currentDateProvider: CurrentDateProvider,
  private val tolgeeProperties: TolgeeProperties,
) : Logging {
  @Scheduled(
    fixedDelayString = "\${tolgee.notifications.cleanup-interval-ms:3600000}",
    initialDelayString = "\${tolgee.notifications.cleanup-interval-ms:3600000}",
  )
  fun deleteOldSeen() {
    lockingProvider.withLockingIfFree(LOCK_NAME, LEASE_TIME) {
      val cutoff =
        currentDateProvider.date.time - Duration.ofDays(tolgeeProperties.notifications.seenRetentionDays).toMillis()
      val deadline = System.currentTimeMillis() + LEASE_TIME.toMillis() * 4 / 5
      var total = 0
      do {
        val deleted =
          jdbcTemplate.update(
            """
            delete from notification where id in (
              select id from notification where seen = true and seen_at < ? limit ?
            )
            """,
            Timestamp(cutoff),
            BATCH_SIZE,
          )
        total += deleted
      } while (deleted == BATCH_SIZE && System.currentTimeMillis() < deadline)
      if (total > 0) {
        logger.info("Notification cleanup removed {} seen notification(s)", total)
      }
    }
  }

  companion object {
    private const val LOCK_NAME = "notification-cleanup"
    private val LEASE_TIME = Duration.ofMinutes(10)
    private const val BATCH_SIZE = 1000
  }
}
