package io.tolgee.ee.service.branching

import io.tolgee.ee.repository.TaskRepository
import io.tolgee.ee.repository.branching.BranchMergeRepository
import io.tolgee.ee.repository.branching.BranchRepository
import io.tolgee.events.OnBranchDeleted
import io.tolgee.repository.KeyRepository
import io.tolgee.service.key.KeyService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Service
class BranchCleanupService(
  private val keyRepository: KeyRepository,
  private val keyService: KeyService,
  private val branchRepository: BranchRepository,
  private val branchMergeRepository: BranchMergeRepository,
  private val taskRepository: TaskRepository,
  private val branchSnapshotService: BranchSnapshotService,
) {
  companion object {
    private const val BATCH_SIZE = 1000
  }

  val logger: Logger by lazy {
    LoggerFactory.getLogger(javaClass)
  }

  /**
   * Event listener triggered after the branch soft deletion transaction completes.
   */
  @Async
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  fun onBranchDeleted(event: OnBranchDeleted) {
    logger.info("Received OnBranchDeleted event for branch ${event.branch.id}")
    cleanupBranchAsync(event.branch.id)
  }

  /**
   * Async cleanup orchestrator.
   * Runs in a separate thread with its own transaction.
   */

  fun cleanupBranchAsync(branchId: Long) {
    try {
      cleanupBranch(branchId)
    } catch (e: Exception) {
      logger.error("Failed to cleanup branch $branchId", e)
      throw e
    }
  }

  /**
   * Synchronous cleanup orchestrator.
   * Ensures all branch-related data is removed in the correct order.
   * Runs in the current transaction if one exists.
   */
  private fun cleanupBranch(branchId: Long) {
    val branch = branchRepository.findById(branchId).orElse(null) ?: return
    if (branch.deletedAt == null) return

    logger.info("Starting cleanup for branch ${branch.name} ($branchId)")

    cleanupBranchKeys(branch.project.id, branchId)
    cleanupBranchSnapshots(branchId)
    cleanupBranchMerges(branchId)
    deleteBranchIfPossible(branchId)

    logger.info("Completed cleanup for branch ${branch.name} ($branchId)")
  }

  /**
   * Deletes all keys associated with the branch in batches.
   * KeyService.deleteMultiple handles cascading deletion of translations, metadata, etc.
   * Always queries page 0 since deletion shifts remaining keys.
   */
  private fun cleanupBranchKeys(
    projectId: Long,
    branchId: Long,
  ) {
    logger.debug("Cleaning up keys for branch $branchId")
    var totalDeleted = 0

    while (true) {
      val idsPage =
        keyRepository.findIdsByProjectAndBranch(
          projectId,
          branchId,
          PageRequest.of(0, BATCH_SIZE),
        )

      if (idsPage.isEmpty) break

      val ids = idsPage.content
      if (ids.isEmpty()) break

      keyService.deleteMultiple(ids)
      totalDeleted += ids.size
      logger.debug("Deleted batch of ${ids.size} keys (total: $totalDeleted) for branch $branchId")
    }

    if (totalDeleted > 0) {
      logger.debug("Completed deletion of $totalDeleted keys for branch $branchId")
    }
  }

  /**
   * Deletes all snapshots created for the branch.
   * Snapshots are used during merge operations to track the original state.
   */
  private fun cleanupBranchSnapshots(branchId: Long) {
    logger.debug("Cleaning up snapshots for branch $branchId")
    branchSnapshotService.deleteSnapshots(branchId)
  }

  /**
   * Deletes all merge records where this branch is either source or target.
   * This includes merge changes and conflict resolutions.
   */
  private fun cleanupBranchMerges(branchId: Long) {
    logger.debug("Cleaning up merges for branch $branchId")

    val merges =
      branchMergeRepository
        .findAllBySourceBranchIdOrTargetBranchId(branchId, branchId)

    if (merges.isNotEmpty()) {
      branchMergeRepository.deleteAll(merges)
      logger.debug("Deleted ${merges.size} merges for branch $branchId")
    }
  }

  /**
   * Deletes the branch entity itself if no tasks reference it.
   * Tasks may still need the branch for historical/audit purposes.
   */
  private fun deleteBranchIfPossible(branchId: Long) {
    val taskCount = taskRepository.countByBranchId(branchId)

    if (taskCount > 0) {
      logger.debug(
        "Skipping branch entity deletion for $branchId because $taskCount tasks still reference it",
      )
      return
    }

    val branch = branchRepository.findById(branchId).orElse(null) ?: return
    branchRepository.delete(branch)
    logger.debug("Deleted branch entity ${branch.name} ($branchId)")
  }
}
