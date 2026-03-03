package io.tolgee.ee.service.branching

import io.tolgee.ee.repository.branching.BranchRepository
import io.tolgee.model.enums.SnapshotStatus
import jakarta.persistence.EntityManager
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.util.Date

/**
 * Handles all transactional work for async snapshot building.
 *
 * Separated from [BranchSnapshotWorker] so that @Async proxying is not on the same
 * bean that needs REQUIRES_NEW nested calls.
 *
 * Self-proxy is obtained via [ApplicationContext.getBean] at call time (not at bean
 * construction time) to avoid BeanCurrentlyInCreationException during startup.
 */
@Service
class BranchSnapshotTxHelper(
  private val branchRepository: BranchRepository,
  private val branchSnapshotService: BranchSnapshotService,
  private val applicationContext: ApplicationContext,
  private val entityManager: EntityManager,
) {
  // Resolved at call-time via the context so there is no circular dependency at startup.
  private fun self() = applicationContext.getBean(BranchSnapshotTxHelper::class.java)

  /**
   * Builds the initial snapshot for a branch under REPEATABLE READ isolation.
   *
   * Returns true if the snapshot was built, false if the branch was not found or
   * was no longer in PENDING state (idempotency guard).
   *
   * After this method returns the caller must invoke [markSnapshotReady] in a
   * separate REQUIRES_NEW transaction to persist the READY status.
   */
  @Transactional(isolation = Isolation.REPEATABLE_READ)
  fun buildSnapshot(branchId: Long): Boolean {
    // Reading the branch establishes the PostgreSQL REPEATABLE READ MVCC snapshot.
    // All subsequent reads in this transaction see the database as of this point in time.
    val branch = branchRepository.findById(branchId).orElse(null) ?: return false
    if (branch.snapshotStatus != SnapshotStatus.PENDING) return false

    val originBranch = branch.originBranch ?: return false

    // Capture projectId while the entity is still managed (project may be lazily loaded).
    val projectId = branch.project.id

    // Release the write lock in a separate transaction so users can start editing.
    // The outer REPEATABLE_READ transaction is unaffected — subsequent key reads still
    // see the creation-time MVCC snapshot, ignoring any edits committed after this point.
    self().releaseWriteLockAndMarkRunning(branchId)

    // Evict all managed entities from the persistence context.
    // Under REPEATABLE_READ, PostgreSQL rejects writes to rows that were modified by a
    // concurrent committed transaction (the REQUIRES_NEW above). Clearing the context
    // prevents Hibernate's auto-flush from attempting to UPDATE the branch row, keeping
    // this transaction purely read-and-insert.
    entityManager.clear()

    branchSnapshotService.createInitialSnapshot(projectId, originBranch, branch)

    return true
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  fun releaseWriteLockAndMarkRunning(branchId: Long) {
    val updated = branchRepository.compareAndSetSnapshotRunning(branchId)
    check(updated > 0) {
      "Branch $branchId is no longer in PENDING state — snapshot may have been claimed by another worker"
    }
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  fun markSnapshotReady(branchId: Long) {
    val branch = branchRepository.findById(branchId).orElse(null) ?: return
    branch.snapshotStatus = SnapshotStatus.READY
    branch.pending = false
    branch.snapshotFinishedAt = Date()
    branchRepository.save(branch)
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  fun markSnapshotFailed(
    branchId: Long,
    errorMessage: String?,
  ) {
    val branch = branchRepository.findById(branchId).orElse(null) ?: return
    branch.snapshotStatus = SnapshotStatus.FAILED
    branch.writeLocked = false
    branch.pending = false
    branch.snapshotErrorMessage = errorMessage
    branch.snapshotFinishedAt = Date()
    branchRepository.save(branch)
  }
}
