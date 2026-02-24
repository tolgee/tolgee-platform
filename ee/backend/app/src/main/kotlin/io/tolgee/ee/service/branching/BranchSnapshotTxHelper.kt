package io.tolgee.ee.service.branching

import io.tolgee.ee.repository.branching.BranchRepository
import io.tolgee.model.enums.SnapshotStatus
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
) {
  // Resolved at call-time via the context so there is no circular dependency at startup.
  private fun self() = applicationContext.getBean(BranchSnapshotTxHelper::class.java)

  @Transactional(isolation = Isolation.REPEATABLE_READ)
  fun buildSnapshot(branchId: Long) {
    // Reading the branch establishes the PostgreSQL REPEATABLE READ MVCC snapshot.
    // All subsequent reads in this transaction see the database as of this point in time.
    val branch = branchRepository.findById(branchId).orElse(null) ?: return
    if (branch.snapshotStatus != SnapshotStatus.PENDING) return

    val originBranch = branch.originBranch ?: return

    // Release the write lock in a separate transaction so users can start editing.
    // The outer REPEATABLE_READ transaction is unaffected — subsequent key reads still
    // see the creation-time MVCC snapshot, ignoring any edits committed after this point.
    self().releaseWriteLockAndMarkRunning(branchId)

    // Sync the in-memory entity to reflect the REQUIRES_NEW commit.
    // Without this, JPA's full UPDATE at the end of this transaction would overwrite
    // writeLocked back to true (its T1 snapshot value), leaving the branch permanently locked.
    branch.writeLocked = false
    branch.snapshotStatus = SnapshotStatus.RUNNING

    branchSnapshotService.createInitialSnapshot(branch.project.id, originBranch, branch)

    branch.snapshotStatus = SnapshotStatus.READY
    branch.snapshotFinishedAt = Date()
    branchRepository.save(branch)
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  fun releaseWriteLockAndMarkRunning(branchId: Long) {
    branchRepository.compareAndSetSnapshotRunning(branchId)
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  fun markSnapshotFailed(
    branchId: Long,
    errorMessage: String?,
  ) {
    val branch = branchRepository.findById(branchId).orElse(null) ?: return
    branch.snapshotStatus = SnapshotStatus.FAILED
    branch.writeLocked = false
    branch.snapshotErrorMessage = errorMessage
    branch.snapshotFinishedAt = Date()
    branchRepository.save(branch)
  }
}
