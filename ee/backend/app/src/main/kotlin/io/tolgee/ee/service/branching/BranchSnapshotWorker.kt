package io.tolgee.ee.service.branching

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service

/**
 * Schedules snapshot builds on a background thread.
 *
 * All transactional work is delegated to [BranchSnapshotTxHelper] to avoid mixing
 * @Async proxy creation with self-injection, which causes circular bean dependency errors.
 *
 * Does NOT implement Logging — @Async creates a JDK dynamic proxy when the bean
 * implements any interface, which then fails type-checking on constructor injection.
 * A static logger avoids the interface and forces a CGLIB subclass proxy instead.
 */
@Service
class BranchSnapshotWorker(
  private val txHelper: BranchSnapshotTxHelper,
) {
  private val log = LoggerFactory.getLogger(BranchSnapshotWorker::class.java)

  @Async
  fun scheduleSnapshot(branchId: Long) {
    try {
      txHelper.buildSnapshot(branchId)
    } catch (e: Exception) {
      log.error("Snapshot build failed for branch $branchId", e)
      txHelper.markSnapshotFailed(branchId, e.message?.take(500))
    }
  }
}
