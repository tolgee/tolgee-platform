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

/**
 * Tracks in-flight branch cleanup tasks so that [io.tolgee.CleanDbTestListener]
 * can wait for them before truncating the database between tests.
 *
 * Separated from [BranchCleanupWorker] because @Async on a bean that implements
 * an interface causes Spring to use a JDK dynamic proxy, which cannot proxy
 * concrete-class methods like [BranchCleanupWorker.scheduleCleanup].
 */
@Component
class BranchCleanupTracker : BackgroundCleanupTracker {
  /** One driver party keeps the Phaser from auto-advancing past the initial phase. */
  private val phaser = Phaser(1)

  /** Call on the calling thread BEFORE submitting the async task. */
  fun register() {
    phaser.register()
  }

  /** Call from the async task when cleanup finishes (success or failure). */
  fun deregister() {
    phaser.arriveAndDeregister()
  }

  /** Blocks until all registered cleanups have called [deregister]. */
  override fun waitForPendingCleanups(timeoutMs: Long) {
    val deadline = System.currentTimeMillis() + timeoutMs
    while (phaser.registeredParties > 1 && System.currentTimeMillis() < deadline) {
      Thread.sleep(50)
    }
  }
}

/**
 * Schedules branch data cleanup on a background thread after a branch is soft-deleted.
 *
 * [scheduleCleanup] must be called within an active transaction. It registers the
 * cleanup tracker immediately (on the calling thread, before any async work) and then
 * hooks into the transaction lifecycle:
 *  - afterCommit  → dispatches the actual cleanup via @Async (through [self])
 *  - afterCompletion (non-commit) → deregisters the tracker so no cleanup is attempted
 *
 * Does NOT implement any interface — @Async requires a CGLIB subclass proxy, which
 * only works when the bean has no JDK-proxy-eligible interfaces.
 */
@Service
class BranchCleanupWorker(
  private val branchCleanupService: BranchCleanupService,
  private val branchCleanupTracker: BranchCleanupTracker,
  /** Self-injected so that [executeCleanup] is called through the Spring proxy,
   *  which enables @Async dispatch to the thread pool. */
  @Lazy private val self: BranchCleanupWorker,
) {
  private val log = LoggerFactory.getLogger(BranchCleanupWorker::class.java)

  /**
   * Must be called within an active transaction.
   *
   * Registers the cleanup tracker immediately (preventing a race where
   * [waitForPendingCleanups] could return before the async task even starts),
   * then schedules [executeCleanup] to run on a background thread after the
   * transaction commits. If the transaction is rolled back the tracker is
   * deregistered without running any cleanup.
   */
  fun scheduleCleanup(
    projectId: Long,
    branchId: Long,
  ) {
    branchCleanupTracker.register()
    TransactionSynchronizationManager.registerSynchronization(
      object : TransactionSynchronization {
        override fun afterCommit() {
          self.executeCleanup(projectId, branchId)
        }

        override fun afterCompletion(status: Int) {
          if (status != TransactionSynchronization.STATUS_COMMITTED) {
            branchCleanupTracker.deregister()
          }
        }
      },
    )
  }

  @Async
  fun executeCleanup(
    projectId: Long,
    branchId: Long,
  ) {
    try {
      branchCleanupService.cleanupBranch(projectId, branchId)
    } catch (e: Exception) {
      log.error("Branch cleanup failed for branch $branchId (project $projectId)", e)
    } finally {
      branchCleanupTracker.deregister()
    }
  }

  /**
   * Blocks until all in-flight cleanups have finished (success or failure).
   */
  fun waitForPendingCleanups(timeoutMs: Long = 30_000) =
    branchCleanupTracker.waitForPendingCleanups(timeoutMs)
}
