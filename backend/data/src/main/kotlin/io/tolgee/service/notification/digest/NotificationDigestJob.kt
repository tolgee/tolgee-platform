package io.tolgee.service.notification.digest

import io.tolgee.component.CurrentDateProvider
import io.tolgee.component.LockingProvider
import io.tolgee.component.email.TolgeeEmailSender
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.dtos.misc.EmailParams
import io.tolgee.model.notifications.NotificationChannel
import io.tolgee.model.notifications.NotificationType
import io.tolgee.service.notification.NotificationDigestStateService
import io.tolgee.service.notification.NotificationEntitiesQueryService
import io.tolgee.service.notification.NotificationSettingsService
import io.tolgee.util.Logging
import io.tolgee.util.executeInNewTransaction
import io.tolgee.util.logger
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.PreparedStatementSetter
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import java.net.URI
import java.sql.Timestamp
import java.time.Duration

@Component
class NotificationDigestJob(
  private val jdbcTemplate: JdbcTemplate,
  private val composer: NotificationDigestComposer,
  private val emailSender: TolgeeEmailSender,
  private val digestStateService: NotificationDigestStateService,
  private val settingsService: NotificationSettingsService,
  private val entitiesQueryService: NotificationEntitiesQueryService,
  private val lockingProvider: LockingProvider,
  private val transactionManager: PlatformTransactionManager,
  private val currentDateProvider: CurrentDateProvider,
  private val tolgeeProperties: TolgeeProperties,
) : Logging {
  private val props get() = tolgeeProperties.notifications

  @Scheduled(
    fixedDelayString = "\${tolgee.notifications.digest-interval-ms:300000}",
    initialDelayString = "\${tolgee.notifications.digest-interval-ms:300000}",
  )
  fun sendDueDigests() {
    if (!smtpConfigured()) return
    lockingProvider.withLockingIfFree(LOCK_NAME, LEASE_TIME) {
      val deadline = System.currentTimeMillis() + LEASE_TIME.toMillis() * 4 / 5
      for ((userId, email) in findDueUsers()) {
        if (System.currentTimeMillis() >= deadline) break
        try {
          sendDigest(userId, email)
        } catch (e: Exception) {
          logger.error("Sending notification digest to user $userId failed", e)
          postponeRetry(userId)
        }
      }
    }
  }

  private fun smtpConfigured(): Boolean {
    val smtp = tolgeeProperties.smtp
    return !smtp.host.isNullOrBlank() && !smtp.from.isNullOrBlank()
  }

  private fun postponeRetry(userId: Long) {
    try {
      digestStateService.markSent(userId, currentDateProvider.date)
    } catch (e: Exception) {
      logger.error("Postponing the notification digest of user $userId failed", e)
    }
  }

  private fun periodStart() =
    Timestamp(currentDateProvider.date.time - Duration.ofHours(props.digestPeriodHours).toMillis())

  private fun sentInsidePeriod(userId: Long): Boolean {
    val lastSentAt =
      jdbcTemplate
        .query(
          "select last_digest_sent_at from notification_digest_state where user_id = ?",
          { rs, _ -> rs.getTimestamp(1) },
          userId,
        ).firstOrNull() ?: return false
    return !lastSentAt.before(periodStart())
  }

  private fun findDueUsers(): List<Pair<Long, String>> {
    val now = currentDateProvider.date.time
    return jdbcTemplate.query(
      """
      select n.user_id, ua.username
      from notification n
      join user_account ua on ua.id = n.user_id and ua.deleted_at is null and ua.disabled_at is null
      join project p on p.id = n.project_id and p.deleted_at is null
      left join notification_digest_state s on s.user_id = n.user_id
      where n.seen = false and n.email_pending = true
        and coalesce(s.frequency, 'DAILY') = 'DAILY'
        and (s.last_digest_sent_at is null or s.last_digest_sent_at < ?)
      group by n.user_id, ua.username
      having max(n.updated_at) < ?
      """,
      { rs, _ -> rs.getLong(1) to rs.getString(2) },
      periodStart(),
      Timestamp(now - props.digestGracePeriodMs),
    )
  }

  private fun sendDigest(
    userId: Long,
    email: String,
  ) {
    val rows = loadRows(userId)
    val (enabled, disabled) =
      rows.partition { settingsService.isEnabled(userId, it.row.type, NotificationChannel.EMAIL) }
    if (disabled.isNotEmpty()) {
      clearPending(disabled)
    }
    if (enabled.isEmpty()) return

    val digest = composer.compose(enabled.map { it.row }, props.entityCap)
    val newestUpdate = enabled.maxOf { it.updatedAt.time }
    if (sentInsidePeriod(userId)) return
    emailSender.sendEmail(
      EmailParams(
        to = email,
        subject = digest.subject,
        text = digest.html,
        messageId = "<digest-$userId-$newestUpdate@${messageIdHost()}>",
      ),
    )
    executeInNewTransaction(transactionManager) {
      clearPending(enabled)
      digestStateService.markSent(userId, currentDateProvider.date)
    }
  }

  private data class LoadedRow(
    val row: DigestRow,
    val updatedAt: Timestamp,
  )

  private data class PendingNotification(
    val id: Long,
    val projectId: Long,
    val projectName: String,
    val type: NotificationType,
    val updatedAt: Timestamp,
  )

  private fun loadRows(userId: Long): List<LoadedRow> {
    val pending =
      jdbcTemplate.query(
        """
        select n.id, p.id, p.name, n.type, n.updated_at
        from notification n
        join project p on p.id = n.project_id and p.deleted_at is null
        where n.user_id = ? and n.seen = false and n.email_pending = true
        order by p.name, n.id
        """,
        { rs, _ ->
          PendingNotification(
            id = rs.getLong(1),
            projectId = rs.getLong(2),
            projectName = rs.getString(3),
            type = NotificationType.valueOf(rs.getString(4)),
            updatedAt = rs.getTimestamp(5),
          )
        },
        userId,
      )
    if (pending.isEmpty()) return emptyList()
    val ids = pending.map { it.id }
    val counts = entitiesQueryService.getCounts(ids)
    val languages = entitiesQueryService.getLanguages(ids)
    val branches = entitiesQueryService.getBranchNames(ids)
    return pending.map {
      LoadedRow(
        row =
          DigestRow(
            notificationId = it.id,
            projectId = it.projectId,
            projectName = it.projectName,
            type = it.type,
            count = counts[it.id] ?: 0,
            languageNames = languages[it.id].orEmpty().map { language -> language.name },
            branchNames = branches[it.id].orEmpty(),
          ),
        updatedAt = it.updatedAt,
      )
    }
  }

  private fun clearPending(rows: List<LoadedRow>) {
    jdbcTemplate.update(
      """
      update notification n
      set email_pending = false
      from unnest(?::bigint[], ?::timestamp[]) as l(id, updated_at)
      where n.id = l.id and n.updated_at <= l.updated_at
      """,
      PreparedStatementSetter { ps ->
        val ids = rows.map { it.row.notificationId }.toTypedArray()
        val timestamps = rows.map { it.updatedAt }.toTypedArray()
        ps.setArray(1, ps.connection.createArrayOf("bigint", ids))
        ps.setArray(2, ps.connection.createArrayOf("timestamp", timestamps))
      },
    )
  }

  private fun messageIdHost(): String =
    tolgeeProperties.frontEndUrl
      ?.let { runCatching { URI(it).host }.getOrNull() }
      ?: smtpFromDomain()
      ?: "tolgee.io"

  private fun smtpFromDomain(): String? =
    tolgeeProperties.smtp.from
      ?.substringAfterLast('@', "")
      ?.takeWhile { it.isLetterOrDigit() || it == '.' || it == '-' }
      ?.takeIf { it.isNotEmpty() }

  companion object {
    private const val LOCK_NAME = "notification-digest"
    private val LEASE_TIME = Duration.ofMinutes(10)
  }
}
