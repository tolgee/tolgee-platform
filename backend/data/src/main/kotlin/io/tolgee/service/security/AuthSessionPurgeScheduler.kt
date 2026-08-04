package io.tolgee.service.security

import io.tolgee.component.CurrentDateProvider
import io.tolgee.component.LockingProvider
import io.tolgee.component.SchedulingManager
import io.tolgee.configuration.tolgee.AuthenticationProperties
import io.tolgee.repository.AuthAuditEventRepository
import io.tolgee.repository.UserSessionRepository
import io.tolgee.util.Logging
import io.tolgee.util.addDays
import io.tolgee.util.executeInNewTransaction
import io.tolgee.util.logger
import io.tolgee.util.runSentryCatching
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import java.time.Duration
import java.util.Date

@Component
class AuthSessionPurgeScheduler(
  private val userSessionRepository: UserSessionRepository,
  private val authAuditEventRepository: AuthAuditEventRepository,
  private val authenticationProperties: AuthenticationProperties,
  private val currentDateProvider: CurrentDateProvider,
  private val lockingProvider: LockingProvider,
  private val transactionManager: PlatformTransactionManager,
  private val schedulingManager: SchedulingManager,
) : Logging {
  @EventListener(ApplicationReadyEvent::class)
  fun schedulePurge() {
    if (!authenticationProperties.sessionAudit.purgeEnabled) {
      logger.info("Auth session purge is disabled, skipping scheduling")
      return
    }

    val period = Duration.ofMillis(authenticationProperties.sessionAudit.purgeDelayMs)
    schedulingManager.scheduleWithFixedDelay(::purge, period)
    logger.debug("Scheduled auth session purge task with period: {}", period)
  }

  fun purge() {
    if (!authenticationProperties.sessionAudit.purgeEnabled) {
      return
    }

    lockingProvider.withLockingIfFree(PURGE_LOCK_NAME, PURGE_LOCK_LEASE_TIME) {
      runSentryCatching {
        purgeAuditEvents()
        purgeSessions()
      }
    }
  }

  /**
   * Sessions age out by expiry only. Purging by revocation date would drop a revoked-but-unexpired
   * session, and the next request with its token would backfill it as active again.
   */
  private fun purgeSessions() {
    val cutoff =
      currentDateProvider.date.addDays(
        -authenticationProperties.sessionAudit.expiredSessionRetentionDays.toInt(),
      )
    val purged =
      purgeInBatches(cutoff) { batchCutoff ->
        val batch =
          executeInNewTransaction(transactionManager) {
            userSessionRepository.findIdsToPurge(batchCutoff, PageRequest.of(0, BATCH_SIZE)).content
          }
        if (batch.isNotEmpty()) {
          executeInNewTransaction(transactionManager) { userSessionRepository.deleteAllByIdIn(batch) }
        }
        batch.size
      }

    if (purged > 0) {
      logger.info("Purged {} expired sessions older than {}", purged, cutoff)
    }
  }

  private fun purgeAuditEvents() {
    val cutoff =
      currentDateProvider.date.addDays(
        -authenticationProperties.sessionAudit.auditEventRetentionDays.toInt(),
      )
    val purged =
      purgeInBatches(cutoff) { batchCutoff ->
        val batch =
          executeInNewTransaction(transactionManager) {
            authAuditEventRepository.findIdsToPurge(batchCutoff, PageRequest.of(0, BATCH_SIZE)).content
          }
        if (batch.isNotEmpty()) {
          executeInNewTransaction(transactionManager) { authAuditEventRepository.deleteAllByIdIn(batch) }
        }
        batch.size
      }

    if (purged > 0) {
      logger.info("Purged {} auth audit events older than {}", purged, cutoff)
    }
  }

  private fun purgeInBatches(
    cutoff: Date,
    purgeBatch: (Date) -> Int,
  ): Int {
    var total = 0
    while (true) {
      val purged = purgeBatch(cutoff)
      total += purged
      if (purged < BATCH_SIZE) return total
    }
  }

  companion object {
    const val BATCH_SIZE = 100
    private const val PURGE_LOCK_NAME = "auth_session_purge_lock"
    private val PURGE_LOCK_LEASE_TIME = Duration.ofMinutes(10)
  }
}
