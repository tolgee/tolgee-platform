package io.tolgee.ee.service.branching

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Lazy
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager

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
  @Lazy private val self: BranchSnapshotWorker,
) {
  private val log = LoggerFactory.getLogger(BranchSnapshotWorker::class.java)

  /**
   * Must be called within an active transaction.
   * Schedules [executeSnapshot] to run on a background thread after the transaction commits.
   */
  fun scheduleSnapshot(branchId: Long) {
    TransactionSynchronizationManager.registerSynchronization(
      object : TransactionSynchronization {
        override fun afterCommit() {
          self.executeSnapshot(branchId)
        }
      },
    )
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
    }
  }
}
