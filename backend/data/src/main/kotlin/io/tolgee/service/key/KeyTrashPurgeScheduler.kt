package io.tolgee.service.key

import io.sentry.Sentry
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
import org.springframework.data.domain.PageRequest
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
    var afterId = 0L
    var totalPurged = 0

    while (true) {
      val batch =
        executeInNewTransaction(transactionManager) {
          keyService.findSoftDeletedIdsDeletedBeforeAndIdAfter(cutoffDate, afterId, PageRequest.of(0, BATCH_SIZE))
        }

      if (batch.isEmpty()) break
      // Advance past every key we attempted, deleted or not, so an un-deletable key can't
      // wedge the run: it is skipped for the rest of this pass and retried on the next one.
      afterId = batch.last()

      totalPurged += purgeBatch(batch)

      if (batch.size < BATCH_SIZE) break
    }

    if (totalPurged > 0) {
      logger.info("Total purged {} expired trashed keys older than {}", totalPurged, cutoffDate)
    }
  }

  private fun purgeBatch(ids: List<Long>): Int {
    try {
      executeInNewTransaction(transactionManager) {
        keyService.hardDeleteMultiple(ids)
      }
      logger.info("Purged {} expired trashed keys", ids.size)
      return ids.size
    } catch (e: Exception) {
      logger.warn("Batch purge of {} trashed keys failed, retrying individually", ids.size, e)
    }
    return ids.count { purgeSingle(it) }
  }

  private fun purgeSingle(id: Long): Boolean {
    try {
      executeInNewTransaction(transactionManager) {
        keyService.hardDeleteMultiple(listOf(id))
      }
      return true
    } catch (e: Exception) {
      logger.error("Failed to purge trashed key {}", id, e)
      Sentry.captureException(e)
      return false
    }
  }

  companion object {
    const val RETENTION_DAYS = 7
    private const val PURGE_LOCK_NAME = "key_trash_purge_lock"
    private const val BATCH_SIZE = 100
    private val PURGE_PERIOD = Duration.ofHours(1)
    private val PURGE_LOCK_LEASE_TIME = Duration.ofMinutes(10)
  }
}
