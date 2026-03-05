package io.tolgee.ee.service.branching

import io.tolgee.Metrics
import io.tolgee.ee.repository.branching.BranchMergeRepository
import io.tolgee.ee.repository.branching.BranchRepository
import io.tolgee.ee.service.TaskService
import io.tolgee.events.OnBranchSoftDeleted
import io.tolgee.repository.LanguageStatsRepository
import io.tolgee.service.contentDelivery.ContentDeliveryConfigService
import io.tolgee.service.key.NamespaceService
import io.tolgee.service.key.ScreenshotService
import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Lazy
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Suppress("SelfReferenceConstructorParameter")
@Service
class BranchCleanupService(
  private val branchRepository: BranchRepository,
  private val branchMergeRepository: BranchMergeRepository,
  private val taskService: TaskService,
  private val branchSnapshotService: BranchSnapshotService,
  private val screenshotService: ScreenshotService,
  private val namespaceService: NamespaceService,
  private val languageStatsRepository: LanguageStatsRepository,
  private val entityManager: EntityManager,
  private val metrics: Metrics,
  @Lazy
  private val contentDeliveryConfigService: ContentDeliveryConfigService,
  @Lazy
  private val self: BranchCleanupService,
) {
  val logger: Logger by lazy {
    LoggerFactory.getLogger(javaClass)
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  @Async
  fun onBranchSoftDeleted(event: OnBranchSoftDeleted) {
    self.cleanupBranch(event.projectId, event.branchId)
  }

  /**
   * Deletes all branch-related data and hard-deletes the branch row.
   *
   * Uses bulk SQL for high-volume tables (translations, key metadata, keys)
   * and delegates to services for business logic (screenshots, namespaces, tasks).
   */
  @Transactional
  fun cleanupBranch(
    projectId: Long,
    branchId: Long,
  ) {
    logger.info("Starting cleanup for branch $branchId")

    contentDeliveryConfigService.deleteAllByBranchId(projectId, branchId)
    taskService.deleteTasksForBranch(projectId, branchId)
    cleanupBranchMerges(branchId)
    cleanupBranchKeys(projectId, branchId)
    languageStatsRepository.deleteAllByBranchId(branchId)
    branchSnapshotService.deleteSnapshots(branchId)
    deleteBranch(branchId)

    logger.info("Completed cleanup for branch $branchId")
    metrics.branchCleanupBatchesCounter.increment()
  }

  /**
   * Deletes all keys and their children for the given branch.
   *
   * Uses bulk SQL for high-volume simple cascades (translations, key metadata, keys)
   * to avoid ORM overhead and the 65K parameter limit.
   * Delegates to services for screenshots (file storage + orphan detection)
   * and namespaces (soft-delete aware cleanup).
   */
  private fun cleanupBranchKeys(
    projectId: Long,
    branchId: Long,
  ) {
    val keySub = "SELECT id FROM key WHERE branch_id = :branchId"

    // --- Translation children + translations (bulk SQL) ---
    execByBranch(
      """
      DELETE FROM translation_comment
      WHERE translation_id IN (SELECT id FROM translation WHERE key_id IN ($keySub))
      """,
      branchId,
    )
    execByBranch(
      """
      DELETE FROM translation_label
      WHERE translation_id IN (SELECT id FROM translation WHERE key_id IN ($keySub))
      """,
      branchId,
    )
    execByBranch(
      """
      UPDATE import_translation SET conflict_id = NULL
      WHERE conflict_id IN (SELECT id FROM translation WHERE key_id IN ($keySub))
      """,
      branchId,
    )
    execByBranch(
      "DELETE FROM translation WHERE key_id IN ($keySub)",
      branchId,
    )

    // --- Key-meta children + key_meta (bulk SQL) ---
    execByBranch(
      "DELETE FROM key_comment WHERE key_meta_id IN (SELECT id FROM key_meta WHERE key_id IN ($keySub))",
      branchId,
    )
    execByBranch(
      "DELETE FROM key_code_reference WHERE key_meta_id IN (SELECT id FROM key_meta WHERE key_id IN ($keySub))",
      branchId,
    )
    execByBranch(
      "DELETE FROM key_meta_tags WHERE key_metas_id IN (SELECT id FROM key_meta WHERE key_id IN ($keySub))",
      branchId,
    )
    execByBranch(
      "DELETE FROM key_meta WHERE key_id IN ($keySub)",
      branchId,
    )

    // --- Screenshots: service deletes files from storage before we remove DB rows ---
    screenshotService.deleteFilesByBranch(branchId)
    // Collect orphan screenshot IDs (only referenced by this branch's keys) before deleting refs.
    @Suppress("UNCHECKED_CAST")
    val orphanScreenshotIds =
      entityManager
        .createNativeQuery(
          """
          SELECT DISTINCT ksr.screenshot_id FROM key_screenshot_reference ksr
          JOIN key k ON k.id = ksr.key_id
          WHERE k.branch_id = :branchId
            AND NOT EXISTS (
              SELECT 1 FROM key_screenshot_reference other
              JOIN key ok ON ok.id = other.key_id
              WHERE other.screenshot_id = ksr.screenshot_id AND ok.branch_id != :branchId
            )
          """.trimIndent(),
        ).setParameter("branchId", branchId)
        .resultList as List<Number>

    execByBranch("DELETE FROM key_screenshot_reference WHERE key_id IN ($keySub)", branchId)
    if (orphanScreenshotIds.isNotEmpty()) {
      entityManager
        .createNativeQuery("DELETE FROM screenshot WHERE id IN (:ids)")
        .setParameter("ids", orphanScreenshotIds.map { it.toLong() })
        .executeUpdate()
    }

    // --- Other key children (bulk SQL) ---
    execByBranch("DELETE FROM task_key WHERE key_id IN ($keySub)", branchId)
    execByBranch("DELETE FROM translation_suggestion WHERE key_id IN ($keySub)", branchId)
    execByBranch("DELETE FROM ai_playground_result WHERE key_id IN ($keySub)", branchId)

    // --- Collect namespaces before deleting keys ---
    @Suppress("UNCHECKED_CAST")
    val namespaceIds =
      entityManager
        .createNativeQuery(
          "SELECT DISTINCT namespace_id FROM key WHERE branch_id = :branchId AND namespace_id IS NOT NULL",
        ).setParameter("branchId", branchId)
        .resultList as List<Number>

    // --- Keys (bulk SQL) ---
    execByBranch("DELETE FROM key WHERE branch_id = :branchId", branchId)

    // --- Namespaces: service handles soft-delete aware cleanup ---
    if (namespaceIds.isNotEmpty()) {
      val namespaces =
        namespaceIds.mapNotNull { id ->
          entityManager.find(io.tolgee.model.key.Namespace::class.java, id.toLong())
        }
      namespaceService.deleteUnusedNamespaces(namespaces)
    }
  }

  private fun cleanupBranchMerges(branchId: Long) {
    val merges =
      branchMergeRepository
        .findAllBySourceBranchIdOrTargetBranchId(branchId, branchId)

    if (merges.isNotEmpty()) {
      branchMergeRepository.deleteAll(merges)
      logger.debug("Deleted ${merges.size} merges for branch $branchId")
    }
  }

  private fun deleteBranch(branchId: Long) {
    val branch = branchRepository.findById(branchId).orElse(null) ?: return
    branchRepository.delete(branch)
  }

  private fun execByBranch(
    sql: String,
    branchId: Long,
  ) {
    entityManager.createNativeQuery(sql.trimIndent()).setParameter("branchId", branchId).executeUpdate()
  }
}
