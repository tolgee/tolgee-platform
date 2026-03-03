package io.tolgee.ee.service.branching

import io.tolgee.Metrics
import io.tolgee.service.contentDelivery.ContentDeliveryConfigService
import io.tolgee.service.key.ScreenshotService
import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service

@Service
class BranchCleanupService(
  private val branchSnapshotService: BranchSnapshotService,
  private val screenshotService: ScreenshotService,
  private val entityManager: EntityManager,
  private val metrics: Metrics,
  @Lazy
  private val contentDeliveryConfigService: ContentDeliveryConfigService,
) {
  val logger: Logger by lazy {
    LoggerFactory.getLogger(javaClass)
  }

  /**
   * Deletes all data associated with a branch and hard-deletes the branch row.
   *
   * The branch has already been soft-deleted (deletedAt set) by the HTTP request,
   * so it is invisible to users. This method runs on a background thread via
   * [BranchCleanupWorker] and uses bulk SQL DELETE statements to avoid the
   * N×1000-batch ORM approach.
   *
   * Deletion order respects FK constraints:
   *   translation children → translations → key_meta children → key_metas
   *   → screenshots → other key children → task children → tasks
   *   → branch_merge children → branch_merges → snapshots → keys
   *   → namespaces (unused) → branch
   */
  @Transactional
  fun cleanupBranch(
    projectId: Long,
    branchId: Long,
  ) {
    logger.info("Starting cleanup for branch $branchId")

    // Content delivery configs have automations with complex cascades — keep existing logic.
    // In practice branches have at most a handful of configs so this is not a bottleneck.
    contentDeliveryConfigService.deleteAllByBranchId(projectId, branchId)

    // Delete screenshot files before removing the DB rows (files have no FK constraints).
    screenshotService.deleteFilesByBranch(branchId)

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

    // --- Translations ---
    exec(
      "DELETE FROM translation WHERE key_id IN (SELECT id FROM key WHERE branch_id = :branchId)",
      "branchId" to branchId,
    )

    // --- Key-meta children ---
    exec(
      """
      DELETE FROM key_comment
      WHERE key_meta_id IN (
        SELECT id FROM key_meta WHERE key_id IN (
          SELECT id FROM key WHERE branch_id = :branchId
        )
      )
      """,
      "branchId" to branchId,
    )
    exec(
      """
      DELETE FROM key_code_reference
      WHERE key_meta_id IN (
        SELECT id FROM key_meta WHERE key_id IN (
          SELECT id FROM key WHERE branch_id = :branchId
        )
      )
      """,
      "branchId" to branchId,
    )
    exec(
      """
      DELETE FROM key_meta_tags
      WHERE key_metas_id IN (
        SELECT id FROM key_meta WHERE key_id IN (
          SELECT id FROM key WHERE branch_id = :branchId
        )
      )
      """,
      "branchId" to branchId,
    )
    exec(
      "DELETE FROM key_meta WHERE key_id IN (SELECT id FROM key WHERE branch_id = :branchId)",
      "branchId" to branchId,
    )

    // --- Screenshots ---
    // Collect IDs of screenshots exclusively referenced by keys in this branch —
    // they will become fully orphaned once we delete the references below.
    @Suppress("UNCHECKED_CAST")
    val orphanScreenshotIds = entityManager.createNativeQuery(
      """
      SELECT DISTINCT ksr.screenshot_id
      FROM key_screenshot_reference ksr
      JOIN key k ON k.id = ksr.key_id
      WHERE k.branch_id = :branchId
        AND NOT EXISTS (
          SELECT 1 FROM key_screenshot_reference other
          JOIN key ok ON ok.id = other.key_id
          WHERE other.screenshot_id = ksr.screenshot_id
            AND ok.branch_id != :branchId
        )
      """.trimIndent(),
    ).setParameter("branchId", branchId).resultList as List<Long>

    exec(
      "DELETE FROM key_screenshot_reference WHERE key_id IN (SELECT id FROM key WHERE branch_id = :branchId)",
      "branchId" to branchId,
    )
    if (orphanScreenshotIds.isNotEmpty()) {
      entityManager.createNativeQuery("DELETE FROM screenshot WHERE id IN (:ids)")
        .setParameter("ids", orphanScreenshotIds)
        .executeUpdate()
    }

    // --- Other key children ---
    exec(
      "DELETE FROM ai_playground_result WHERE key_id IN (SELECT id FROM key WHERE branch_id = :branchId)",
      "branchId" to branchId,
    )

    // --- task_key must be deleted before both tasks and keys ---
    exec(
      """
      DELETE FROM task_key
      WHERE task_id IN (
        SELECT id FROM task WHERE project_id = :projectId AND branch_id = :branchId
      )
      """,
      "projectId" to projectId,
      "branchId" to branchId,
    )
    exec(
      """
      DELETE FROM task_assignees
      WHERE tasks_id IN (
        SELECT id FROM task WHERE project_id = :projectId AND branch_id = :branchId
      )
      """,
      "projectId" to projectId,
      "branchId" to branchId,
    )
    exec(
      "DELETE FROM task WHERE project_id = :projectId AND branch_id = :branchId",
      "projectId" to projectId,
      "branchId" to branchId,
    )

    // --- Branch merge changes & merges ---
    // Delete changes belonging to this branch's merges.
    exec(
      """
      DELETE FROM branch_merge_change
      WHERE branch_merge_id IN (
        SELECT id FROM branch_merge
        WHERE source_branch_id = :branchId OR target_branch_id = :branchId
      )
      """,
      "branchId" to branchId,
    )
    // Also delete any remaining changes referencing keys on this branch
    // (e.g. from merges between other branches that referenced these keys).
    exec(
      """
      DELETE FROM branch_merge_change
      WHERE source_key_id IN (SELECT id FROM key WHERE branch_id = :branchId)
         OR target_key_id IN (SELECT id FROM key WHERE branch_id = :branchId)
      """,
      "branchId" to branchId,
    )
    exec(
      "DELETE FROM branch_merge WHERE source_branch_id = :branchId OR target_branch_id = :branchId",
      "branchId" to branchId,
    )

    // --- Snapshots (already bulk SQL inside deleteSnapshots) ---
    branchSnapshotService.deleteSnapshots(branchId)

    // --- Language stats ---
    exec(
      "DELETE FROM language_stats WHERE branch_id = :branchId",
      "branchId" to branchId,
    )

    // --- Keys ---
    exec(
      "DELETE FROM key WHERE branch_id = :branchId",
      "branchId" to branchId,
    )

    // --- Namespaces no longer referenced by any key in the project ---
    exec(
      """
      DELETE FROM namespace
      WHERE project_id = :projectId
        AND id NOT IN (
          SELECT namespace_id FROM key
          WHERE project_id = :projectId AND namespace_id IS NOT NULL
        )
      """,
      "projectId" to projectId,
    )

    // --- Hard-delete the branch row ---
    exec(
      "DELETE FROM branch WHERE id = :branchId",
      "branchId" to branchId,
    )

    logger.info("Completed cleanup for branch $branchId")
    metrics.branchCleanupBatchesCounter.increment()
  }

  private fun exec(
    sql: String,
    vararg params: Pair<String, Any>,
  ) {
    val query = entityManager.createNativeQuery(sql.trimIndent())
    params.forEach { (name, value) -> query.setParameter(name, value) }
    query.executeUpdate()
  }
}
