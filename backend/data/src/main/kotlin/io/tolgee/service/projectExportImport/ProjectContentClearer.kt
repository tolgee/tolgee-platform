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
 * Deletes all of a project's in-scope content in place, keeping the project row (its id anchors the
 * re-imported FKs), its access (permissions, API keys), organization ownership, and the project-level
 * config that is not transferred (automations, webhooks, content delivery, Slack). Step 0 of the mirror
 * import: afterwards the project owns no content, so the export inserts with no merge/upsert.
 *
 * Deletion order follows [io.tolgee.service.project.ProjectHardDeletingService] (the schema has almost no
 * `ON DELETE CASCADE`, so children go before parents). [assertCleared] proves completeness at runtime, and
 * the `clear strategy` build guard forces every OWNED type into [CLEARED_OWNED_TYPES].
 *
 * Not `@Transactional`: it must run inside the importer's transaction so the wipe and re-insert share one
 * rollback boundary.
 *
 * Each non-OWNED wipe below carries a disposition tag (an untagged delete is an OWNED type restored via
 * the generic graph): `DROPPED` (gone for good), `RESTORED_DEFAULT` (re-seeded by the importer), or
 * `RESTORED_SIDE_CHANNEL` (re-inserted from a dedicated export file).
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
    // Child rows cascade via @OneToMany, so these must run after the join tables above and before the
    // project row.
    languageService.deleteAllByProject(projectId)
    keyService.deleteAllByProject(projectId)

    // Bulk JPQL/native deletes above bypass the persistence context; sync it before the entity-based
    // branch removal and the recount so neither sees stale rows (Hibernate 6.6 CHECK_ON_FLUSH).
    entityManager.flush()
    entityManager.clear()

    // RESTORED_SIDE_CHANNEL: importer re-inserts remapped rows via restoreSideChannels.
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
    // Snapshot and merge rows FK branch (no DB cascade), so they must go before the branch rows. Mirrors
    // the table relationships in the EE branch-scoped BranchCleanupService.
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
   * returns nothing. The caller must `flush()`+`clear()` first, or the bulk deletes' bypassed persistence
   * context reads stale.
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
     * of OWNED types, so a newly classified OWNED entity can't ship until its deletion is wired here.
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
