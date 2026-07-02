package io.tolgee.service.projectExportImport

import io.tolgee.model.Language
import io.tolgee.model.Project
import io.tolgee.model.Screenshot
import io.tolgee.model.TranslationSuggestion
import io.tolgee.model.branching.Branch
import io.tolgee.model.branching.snapshot.KeyMetaSnapshot
import io.tolgee.model.branching.snapshot.KeySnapshot
import io.tolgee.model.branching.snapshot.TranslationSnapshot
import io.tolgee.model.key.Key
import io.tolgee.model.key.KeyCodeReference
import io.tolgee.model.key.KeyComment
import io.tolgee.model.key.KeyMeta
import io.tolgee.model.key.Namespace
import io.tolgee.model.key.Tag
import io.tolgee.model.key.screenshotReference.KeyScreenshotReference
import io.tolgee.model.qa.LanguageQaConfig
import io.tolgee.model.qa.ProjectQaConfig
import io.tolgee.model.qa.TranslationQaIssue
import io.tolgee.model.task.Task
import io.tolgee.model.task.TaskKey
import io.tolgee.model.translation.Label
import io.tolgee.model.translation.Translation
import io.tolgee.model.translation.TranslationComment
import io.tolgee.repository.ProjectRepository
import io.tolgee.repository.qa.ProjectQaConfigRepository
import io.tolgee.service.AiPlaygroundResultService
import io.tolgee.service.bigMeta.BigMetaService
import io.tolgee.service.branching.BranchService
import io.tolgee.service.dataImport.ImportService
import io.tolgee.service.dataImport.ImportSettingsService
import io.tolgee.service.key.KeyService
import io.tolgee.service.label.LabelService
import io.tolgee.service.language.LanguageService
import io.tolgee.service.machineTranslation.MtServiceConfigService
import io.tolgee.service.translationMemory.TranslationMemoryManagementService
import io.tolgee.util.Logging
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Component
import kotlin.reflect.KClass

/**
 * Deletes all of a project's in-scope content **in place**, keeping the project row (its id anchors the
 * re-imported FKs), its access (permissions, API keys), organization ownership, and the project-level
 * config that is intentionally not transferred (automations, webhooks, content delivery, Slack). This is
 * step 0 of the mirror import: after it runs the project holds none of its OWNED content, so the export
 * inserts cleanly with no merge/upsert.
 *
 * Ordering reuses the proven sequence of [io.tolgee.service.project.ProjectHardDeletingService] — the
 * schema has almost no `ON DELETE CASCADE`, so children must go before parents — and adds the pieces that
 * service never needed: project-scoped deletion of tasks/task-keys and of the branch merge/snapshot
 * tables, plus detaching kept config whose FK points at a branch being wiped. Completeness is proven at
 * runtime by [assertCleared]; a new OWNED type that this clearer forgets fails that assertion (and the
 * `clear strategy` build guard forces it into [CLEARED_OWNED_TYPES]).
 *
 * Must be called within the caller's transaction (the importer's): it is deliberately not `@Transactional`
 * so the wipe and the subsequent re-insert share one rollback boundary, and its flush/clear + `COUNT == 0`
 * backstop only make sense inside that transaction.
 *
 * This is a **mirror transfer, not a backup/restore**: everything project-scoped is wiped so no
 * base-project row can interfere with the insert, but only in-scope data is restored — so a
 * non-OWNED wipe with no restore path is the normal, intended case, not an omission. Each non-OWNED
 * wipe below carries an inline disposition tag (an untagged delete is an OWNED type, restored via the
 * generic graph); a future editor adding a non-OWNED wipe should tag it with one of:
 *  - `DROPPED` — gone for good (transient or user-reconfigurable data),
 *  - `RESTORED_DEFAULT` — re-seeded by the importer as project creation would,
 *  - `RESTORED_SIDE_CHANNEL` — re-inserted by the importer from a dedicated export file.
 */
@Component
class ProjectContentClearer(
  private val entityManager: EntityManager,
  private val projectRepository: ProjectRepository,
  private val translationMemoryManagementService: TranslationMemoryManagementService,
  private val importService: ImportService,
  private val importSettingsService: ImportSettingsService,
  private val mtServiceConfigService: MtServiceConfigService,
  private val aiPlaygroundResultService: AiPlaygroundResultService,
  private val labelService: LabelService,
  private val languageService: LanguageService,
  private val keyService: KeyService,
  private val bigMetaService: BigMetaService,
  private val branchService: BranchService,
  private val projectQaConfigRepository: ProjectQaConfigRepository,
) : Logging {
  fun clear(project: Project) {
    val projectId = project.id

    clearTasks(projectId)
    // branch_merge_change FKs key (no cascade) — must go before keys, and before its branch_merge parent.
    deleteBranchMergeChanges(projectId) // DROPPED (branch-merge history)
    clearImports(projectId) // DROPPED (transient upload state)
    // RESTORED_DEFAULT: importer re-seeds a fresh PROJECT-type TM via createProjectTm.
    translationMemoryManagementService.deleteAllByProject(projectId)

    // The project keeps pointing at content we are about to delete; null the pointers first.
    project.baseLanguage = null
    project.defaultNamespace = null
    projectRepository.saveAndFlush(project)

    mtServiceConfigService.deleteAllByProjectId(projectId) // DROPPED (user-reconfigurable)
    aiPlaygroundResultService.deleteResultsByProject(projectId) // DROPPED (transient)
    // translation_label rows FK translation; clearing labels drops the join before translations are deleted.
    labelService.deleteLabelsByProjectId(projectId)
    deleteSuggestions(projectId)
    // These two reuse the language/key delete subgraphs; their child rows cascade per the entities'
    // @OneToMany mappings, so they must run before the project row but after the join tables above.
    languageService.deleteAllByProject(projectId)
    keyService.deleteAllByProject(projectId)

    // Bulk JPQL/native deletes above bypass the persistence context; sync it before the entity-based
    // branch removal and the recount so neither sees stale rows (Hibernate 6.6 CHECK_ON_FLUSH).
    entityManager.flush()
    entityManager.clear()

    // RESTORED_SIDE_CHANNEL: importer re-inserts remapped rows via restoreBigMeta.
    bigMetaService.deleteAllByProjectId(projectId)
    detachKeptConfigFromBranches(projectId)
    deleteBranchMergesAndSnapshots(projectId) // branch_merge is DROPPED (history); the snapshot rows are OWNED
    branchService.deleteAllByProjectId(projectId)
    projectQaConfigRepository.deleteAllByProjectId(projectId)

    entityManager.flush()
    entityManager.clear()
    assertCleared(projectId)
  }

  private fun clearTasks(projectId: Long) {
    // No project-scoped task delete exists. task_key FKs both task and key; deleting it here (by task's
    // project) lets keys delete later and lets tasks delete now, before languages (Task.language, NOT NULL)
    // and branches (Task.branch) are removed.
    nativeUpdate(
      "DELETE FROM task_assignees WHERE tasks_id IN (SELECT id FROM task WHERE project_id = :projectId)",
      projectId,
    )
    jpqlUpdate("DELETE FROM TaskKey tk WHERE tk.task.project.id = :projectId", projectId)
    jpqlUpdate("DELETE FROM Task t WHERE t.project.id = :projectId", projectId)
  }

  private fun deleteBranchMergeChanges(projectId: Long) {
    nativeUpdate(
      "DELETE FROM branch_merge_change WHERE branch_merge_id IN (" +
        "SELECT bm.id FROM branch_merge bm WHERE " +
        "bm.source_branch_id IN (SELECT id FROM branch WHERE project_id = :projectId) OR " +
        "bm.target_branch_id IN (SELECT id FROM branch WHERE project_id = :projectId))",
      projectId,
    )
  }

  private fun clearImports(projectId: Long) {
    // Imports FK both branch and language (no cascade) and are transient upload state, never transferred.
    importService.getAllByProject(projectId).forEach { importService.hardDeleteImport(it) }
    importSettingsService.deleteAllByProject(projectId)
  }

  private fun deleteSuggestions(projectId: Long) {
    // TranslationSuggestionServiceOssImpl.deleteAllByProject is a no-op on OSS, so the languageService
    // cascade won't remove suggestions here — delete them explicitly or they leak into the mirror import.
    jpqlUpdate("DELETE FROM TranslationSuggestion ts WHERE ts.project.id = :projectId", projectId)
  }

  private fun detachKeptConfigFromBranches(projectId: Long) {
    // content_delivery_config is kept (project-level config, not content) but its nullable branch FK would
    // block the branch wipe; detach it. A re-import recreates branches, so a branch-pinned delivery config
    // falls back to the default branch — acceptable for a wipe-and-replace.
    nativeUpdate(
      "UPDATE content_delivery_config SET branch_id = NULL WHERE project_id = :projectId",
      projectId,
    )
  }

  private fun deleteBranchMergesAndSnapshots(projectId: Long) {
    // Snapshots FK branch (and chain translation/keyMeta snapshots off the key snapshot); merges FK branch.
    // All must go before the branch rows; the EE feature creates an initial snapshot per branch. The same
    // table relationships are encoded in the EE BranchCleanupService (branch-scoped); keep both in sync if
    // the branch-snapshot/merge schema changes.
    nativeUpdate(
      "DELETE FROM branch_translation_snapshot WHERE key_snapshot_id IN " +
        "(SELECT id FROM branch_key_snapshot WHERE project_id = :projectId)",
      projectId,
    )
    nativeUpdate(
      "DELETE FROM branch_key_meta_snapshot WHERE key_snapshot_id IN " +
        "(SELECT id FROM branch_key_snapshot WHERE project_id = :projectId)",
      projectId,
    )
    nativeUpdate("DELETE FROM branch_key_snapshot WHERE project_id = :projectId", projectId)
    nativeUpdate(
      "DELETE FROM branch_merge WHERE " +
        "source_branch_id IN (SELECT id FROM branch WHERE project_id = :projectId) OR " +
        "target_branch_id IN (SELECT id FROM branch WHERE project_id = :projectId)",
      projectId,
    )
    // Detach the branch self-reference so the entity-based branch delete below can't FK-violate on an
    // origin branch that happens to be removed first (the FK has no DB-level ON DELETE).
    nativeUpdate("UPDATE branch SET origin_branch_id = NULL WHERE project_id = :projectId", projectId)
  }

  /**
   * Proves the wipe is complete by running every OWNED type's project-scoped collector and asserting it
   * returns nothing. Driven by the collectors (not a graph walk) so it catches exactly the unreachable
   * types (`ProjectQaConfig`, `LanguageQaConfig`, unassigned `Label`) a missed clear path would leak. The
   * caller must `flush()`+`clear()` first, or the bulk deletes' bypassed persistence context reads stale.
   */
  private fun assertCleared(projectId: Long) {
    ProjectScopedCollectorQueries.queriesByClassName.forEach { (className, jpql) ->
      val remaining =
        entityManager
          .createQuery(jpql)
          .setParameter("projectId", projectId)
          .setMaxResults(1)
          .resultList
      check(remaining.isEmpty()) {
        "Clear-in-place left ${remaining.size}+ rows of $className for project $projectId; its delete path " +
          "in ProjectContentClearer is missing or incomplete (mirror import would corrupt into a merge)."
      }
    }
  }

  private fun jpqlUpdate(
    jpql: String,
    projectId: Long,
  ) {
    entityManager.createQuery(jpql).setParameter("projectId", projectId).executeUpdate()
  }

  private fun nativeUpdate(
    sql: String,
    projectId: Long,
  ) {
    entityManager.createNativeQuery(sql).setParameter("projectId", projectId).executeUpdate()
  }

  companion object {
    /**
     * Every OWNED type this clearer removes. The `clear strategy` build guard asserts this equals the set
     * of OWNED types, so a newly classified OWNED entity can't ship until a developer wires its deletion
     * here and records it in this set.
     */
    val CLEARED_OWNED_TYPES: Set<KClass<*>> =
      setOf(
        Language::class,
        Namespace::class,
        Key::class,
        KeyMeta::class,
        KeyComment::class,
        KeyCodeReference::class,
        Translation::class,
        TranslationComment::class,
        TranslationSuggestion::class,
        Tag::class,
        Label::class,
        Screenshot::class,
        KeyScreenshotReference::class,
        Branch::class,
        Task::class,
        TaskKey::class,
        ProjectQaConfig::class,
        LanguageQaConfig::class,
        TranslationQaIssue::class,
        // Deleted by deleteBranchMergesAndSnapshots (before the branch wipe).
        KeySnapshot::class,
        TranslationSnapshot::class,
        KeyMetaSnapshot::class,
      )

    val clearedOwnedClassNames: Set<String>
      get() = CLEARED_OWNED_TYPES.map { it.java.name }.toSet()
  }
}
