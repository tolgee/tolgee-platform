package io.tolgee.ee.service.branching

import io.tolgee.component.BackgroundCleanupTracker
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Lazy
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.util.concurrent.Phaser

@Component
class BranchSnapshotTracker : BackgroundCleanupTracker {
  private val phaser = Phaser(1)

  fun register() {
    phaser.register()
  }

  fun deregister() {
    phaser.arriveAndDeregister()
  }

  override fun waitForPendingCleanups(timeoutMs: Long) {
    val deadline = System.currentTimeMillis() + timeoutMs
    while (phaser.registeredParties > 1 && System.currentTimeMillis() < deadline) {
      Thread.sleep(50)
    }
    check(phaser.registeredParties <= 1) {
      "Timed out waiting for ${phaser.registeredParties - 1} pending branch snapshot(s) after ${timeoutMs}ms"
    }
  }
}

/**
 * Schedules snapshot builds on a background thread.
 *
 * [scheduleSnapshot] must be called within an active transaction. It hooks into
 * the transaction lifecycle and dispatches [executeSnapshot] (@Async) after commit
 * via [self] — a @Lazy self-reference that ensures the call goes through the Spring
 * proxy so that @Async is intercepted correctly.
 *
 * All transactional work is delegated to [BranchSnapshotTxHelper] to keep the
 * @Async proxy (CGLIB) from having to also be @Transactional.
 */
@Service
class BranchSnapshotWorker(
  private val txHelper: BranchSnapshotTxHelper,
  private val branchSnapshotTracker: BranchSnapshotTracker,
  @Lazy private val self: BranchSnapshotWorker,
) {
  private val log = LoggerFactory.getLogger(BranchSnapshotWorker::class.java)

  /**
   * Must be called within an active transaction.
   * Schedules [executeSnapshot] to run on a background thread after the transaction commits.
   */
  fun scheduleSnapshot(branchId: Long) {
    branchSnapshotTracker.register()
    try {
      TransactionSynchronizationManager.registerSynchronization(
        object : TransactionSynchronization {
          override fun afterCommit() {
            self.executeSnapshot(branchId)
          }

          override fun afterCompletion(status: Int) {
            if (status != TransactionSynchronization.STATUS_COMMITTED) {
              branchSnapshotTracker.deregister()
            }
          }
        },
      )
    } catch (e: Exception) {
      branchSnapshotTracker.deregister()
      throw e
    }
  }

  @Async
  fun executeSnapshot(branchId: Long) {
    try {
      val built = txHelper.buildSnapshot(branchId)
      if (built) {
        txHelper.markSnapshotReady(branchId)
      }
    } catch (e: Exception) {
      log.error("Snapshot build failed for branch $branchId", e)
      txHelper.markSnapshotFailed(branchId, e.message?.take(500))
    } finally {
      branchSnapshotTracker.deregister()
    }
  }
}
