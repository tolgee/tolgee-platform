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
      val deleted =
        jdbcTemplate.update(
          "delete from notification where seen = true and updated_at < ?",
          Timestamp(cutoff),
        )
      if (deleted > 0) {
        logger.info("Notification cleanup removed {} seen notification(s)", deleted)
      }
    }
  }

  companion object {
    private const val LOCK_NAME = "notification-cleanup"
    private val LEASE_TIME = Duration.ofMinutes(10)
  }
}
