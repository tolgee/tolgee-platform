package io.tolgee.service.key

import io.tolgee.component.CurrentDateProvider
import io.tolgee.component.LockingProvider
import io.tolgee.component.SchedulingManager
import io.tolgee.util.Logging
import io.tolgee.util.addDays
import io.tolgee.util.executeInNewTransaction
import io.tolgee.util.logger
import io.tolgee.util.runSentryCatching
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import java.time.Duration

@Component
class KeyTrashPurgeScheduler(
  private val keyService: KeyService,
  private val currentDateProvider: CurrentDateProvider,
  private val lockingProvider: LockingProvider,
  private val transactionManager: PlatformTransactionManager,
  private val schedulingManager: SchedulingManager,
) : Logging {
  @EventListener(ApplicationReadyEvent::class)
  fun schedulePurge() {
    schedulingManager.scheduleWithFixedDelay(::purge, PURGE_PERIOD)
    logger.debug("Scheduled key trash purge task with period: {}", PURGE_PERIOD)
  }

  fun purge() {
    lockingProvider.withLockingIfFree(PURGE_LOCK_NAME, PURGE_LOCK_LEASE_TIME) {
      runSentryCatching {
        purgeExpiredKeys()
      }
    }
  }

  private fun purgeExpiredKeys() {
    val cutoffDate = currentDateProvider.date.addDays(-RETENTION_DAYS)
    val expiredKeys =
      executeInNewTransaction(transactionManager) {
        keyService.findAllSoftDeletedBefore(cutoffDate)
      }

    if (expiredKeys.isEmpty()) return

    expiredKeys.map { it.id }.chunked(BATCH_SIZE).forEach { batch ->
      executeInNewTransaction(transactionManager) {
        keyService.hardDeleteMultiple(batch)
      }
      logger.info("Purged {} expired trashed keys", batch.size)
    }

    logger.info("Total purged {} expired trashed keys older than {}", expiredKeys.size, cutoffDate)
  }

  companion object {
    private const val PURGE_LOCK_NAME = "key_trash_purge_lock"
    private const val RETENTION_DAYS = 7
    private const val BATCH_SIZE = 100
    private val PURGE_PERIOD = Duration.ofHours(1)
    private val PURGE_LOCK_LEASE_TIME = Duration.ofMinutes(10)
  }
}
