package io.tolgee.ee.service.branching

import io.tolgee.Metrics
import io.tolgee.ee.repository.branching.BranchMergeRepository
import io.tolgee.ee.repository.branching.BranchRepository
import io.tolgee.ee.service.TaskService
import io.tolgee.repository.KeyRepository
import io.tolgee.repository.LanguageStatsRepository
import io.tolgee.service.key.KeyService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service

@Service
class BranchCleanupService(
  private val keyRepository: KeyRepository,
  private val keyService: KeyService,
  private val branchRepository: BranchRepository,
  private val branchMergeRepository: BranchMergeRepository,
  private val taskService: TaskService,
  private val branchSnapshotService: BranchSnapshotService,
  private val languageStatsRepository: LanguageStatsRepository,
  private val metrics: Metrics,
) {
  companion object {
    private const val BATCH_SIZE = 1000
  }

  val logger: Logger by lazy {
    LoggerFactory.getLogger(javaClass)
  }

  /**
   * Synchronously deletes all branch-related data and the branch entity itself.
   * Removes tasks, keys, merges, snapshots, and then hard-deletes the branch.
   */
  fun cleanupBranch(
    projectId: Long,
    branchId: Long,
  ) {
    logger.info("Starting cleanup for branch $branchId")

    cleanupBranchTasks(projectId, branchId)
    cleanupBranchKeys(projectId, branchId)
    cleanupBranchMerges(branchId)
    cleanupLanguageStats(branchId)
    cleanupBranchSnapshots(branchId)
    deleteBranch(branchId)

    logger.info("Completed cleanup for branch $branchId")
  }

  /**
   * Deletes all tasks associated with the branch.
   */
  private fun cleanupBranchTasks(
    projectId: Long,
    branchId: Long,
  ) {
    taskService.deleteTasksForBranch(projectId, branchId)
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
    var totalDeleted = 0
    var batchCount = 0

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
      batchCount++
      metrics.branchCleanupBatchesCounter.increment()
    }

    if (totalDeleted > 0) {
      logger.debug("Deleted $totalDeleted keys in $batchCount batches for branch $branchId")
    }
  }

  /**
   * Deletes all merge records where this branch is either source or target.
   * This includes merge changes and conflict resolutions.
   */
  private fun cleanupBranchMerges(branchId: Long) {
    val merges =
      branchMergeRepository
        .findAllBySourceBranchIdOrTargetBranchId(branchId, branchId)

    if (merges.isNotEmpty()) {
      branchMergeRepository.deleteAll(merges)
      logger.debug("Deleted ${merges.size} merges for branch $branchId")
    }
  }

  /**
   * Deletes all language stats associated with the branch.
   */
  private fun cleanupLanguageStats(branchId: Long) {
    languageStatsRepository.deleteAllByBranchId(branchId)
  }

  /**
   * Deletes all snapshots created for the branch.
   */
  private fun cleanupBranchSnapshots(branchId: Long) {
    branchSnapshotService.deleteSnapshots(branchId)
  }

  /**
   * Hard-deletes the branch entity.
   */
  private fun deleteBranch(branchId: Long) {
    val branch = branchRepository.findById(branchId).orElse(null) ?: return
    branchRepository.delete(branch)
  }
}
