package io.tolgee.ee.component.qa

import io.tolgee.activity.data.RevisionType
import io.tolgee.batch.BatchJobService
import io.tolgee.batch.OnBatchJobCompleted
import io.tolgee.batch.data.BatchJobType
import io.tolgee.batch.data.BatchTranslationTargetItem
import io.tolgee.batch.events.OnBatchJobCancelled
import io.tolgee.batch.events.OnBatchJobFailed
import io.tolgee.batch.events.OnBatchJobSucceeded
import io.tolgee.batch.request.QaCheckRequest
import io.tolgee.component.enabledFeaturesProvider.EnabledFeaturesProvider
import io.tolgee.constants.Feature
import io.tolgee.ee.service.qa.QaRecheckService
import io.tolgee.events.OnProjectActivityEvent
import io.tolgee.model.Language
import io.tolgee.model.Project
import io.tolgee.model.translation.Translation
import io.tolgee.service.language.LanguageService
import io.tolgee.service.project.ProjectFeatureGuard
import io.tolgee.service.project.ProjectService
import io.tolgee.service.translation.TranslationService
import io.tolgee.util.runSentryCatching
import jakarta.persistence.EntityManager
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

/**
 * QA event listener that reacts to:
 * - [OnProjectActivityEvent]: entity modifications (translation text
 *   changes, project creation, language tag changes, base language changes)
 * - [OnBatchJobCompleted]: batch job completions for bulk translation text modifications
 *
 * Stale marking (qaChecksStale = true) runs unconditionally — even when the QA feature is disabled.
 * This is needed so translations have the correct stale state when QA is enabled later
 * and when batch jobs complete.
 */
@Component
class QaActivityListener(
  private val batchJobService: BatchJobService,
  private val entityManager: EntityManager,
  private val projectFeatureGuard: ProjectFeatureGuard,
  private val projectService: ProjectService,
  private val languageService: LanguageService,
  private val translationService: TranslationService,
  private val qaRecheckService: QaRecheckService,
  private val enabledFeaturesProvider: EnabledFeaturesProvider,
) {
  @EventListener
  fun onActivity(event: OnProjectActivityEvent) {
    runSentryCatching {
      val projectId = event.activityRevision.projectId ?: return
      val isBatchChunk = event.activityRevision.batchJobChunkExecution != null

      // Mark translations stale unconditionally - even when QA is disabled and even when processing batch chunks
      markTranslationsStale(event, projectId)

      // If this activity is part of a batch chunk, we'll handle it when a batch job completes
      if (isBatchChunk) return

      // Enable QA checks for new projects if the feature is available
      handleProjectCreated(event, projectId)

      val project = projectService.findDto(projectId) ?: return
      if (!projectFeatureGuard.isFeatureEnabled(Feature.QA_CHECKS, project)) return

      handleLanguageTagChanged(event, projectId)
      handleBaseLanguageChanged(event, projectId)
      handleTranslationChanged(event, projectId)
    }
  }

  @EventListener(OnBatchJobSucceeded::class)
  fun onBatchJobSucceeded(event: OnBatchJobSucceeded) {
    handleBatchJobCompleted(event)
  }

  @EventListener(OnBatchJobFailed::class)
  fun onBatchJobFailed(event: OnBatchJobFailed) {
    handleBatchJobCompleted(event)
  }

  @EventListener(OnBatchJobCancelled::class)
  fun onBatchJobCancelled(event: OnBatchJobCancelled) {
    handleBatchJobCompleted(event)
  }

  /**
   * Marks translations as stale. Runs unconditionally — no feature guard, works during batch chunks.
   * Marks both direct translations (where text changed) and siblings (other languages when base text changes).
   */
  private fun markTranslationsStale(
    event: OnProjectActivityEvent,
    projectId: Long,
  ) {
    val textChangedTranslationIds = getTextChangedTranslationIds(event)
    if (textChangedTranslationIds.isEmpty()) return

    // Mark direct translations as stale
    translationService.setQaChecksStale(textChangedTranslationIds)

    // Mark siblings of base language changes as stale
    val baseLanguage = languageService.getProjectBaseLanguage(projectId)
    val siblingIds =
      translationService.getSiblingIdsForBaseLanguageChanges(
        translationIds = textChangedTranslationIds,
        baseLanguageId = baseLanguage.id,
      )
    if (siblingIds.isNotEmpty()) {
      translationService.setQaChecksStale(siblingIds)
    }
  }

  private fun handleTranslationChanged(
    event: OnProjectActivityEvent,
    projectId: Long,
  ) {
    val translationEntities = event.modifiedEntities[Translation::class] ?: return

    // Find translations where "text" was modified (both new and updated, excluding deletes)
    val textChangedEntities =
      translationEntities.filter { (_, entity) ->
        entity.revisionType != RevisionType.DEL &&
          entity.modifications.containsKey("text")
      }

    if (textChangedEntities.isEmpty()) return

    // Extract direct targets from describing relations
    val directTargets =
      textChangedEntities.values.mapNotNull { entity ->
        val keyId = entity.describingRelations?.get("key")?.entityId ?: return@mapNotNull null
        val languageId = entity.describingRelations?.get("language")?.entityId ?: return@mapNotNull null
        BatchTranslationTargetItem(keyId = keyId, languageId = languageId)
      }

    if (directTargets.isEmpty()) return

    // For base text changes, also include all project languages
    val baseLanguage = languageService.getProjectBaseLanguage(projectId)
    val baseLanguageKeyIds =
      directTargets
        .filter { it.languageId == baseLanguage.id }
        .map { it.keyId }
        .toSet()

    val siblingTargets =
      if (baseLanguageKeyIds.isNotEmpty()) {
        val otherLanguageIds =
          languageService
            .getProjectLanguages(projectId)
            .map { it.id }
            .filter { it != baseLanguage.id }
        baseLanguageKeyIds.flatMap { keyId ->
          otherLanguageIds.map { langId -> BatchTranslationTargetItem(keyId = keyId, languageId = langId) }
        }
      } else {
        emptyList()
      }

    val allTargets = (directTargets + siblingTargets).distinct()

    // Chunk and start batch jobs
    val project = entityManager.getReference(Project::class.java, projectId)
    for (chunk in allTargets.chunked(QaRecheckService.MAX_BATCH_JOB_TARGET_SIZE)) {
      batchJobService.startJob(
        request = QaCheckRequest(target = chunk),
        project = project,
        author = null,
        type = BatchJobType.QA_CHECK,
        isHidden = true,
      )
    }
  }

  private fun getTextChangedTranslationIds(event: OnProjectActivityEvent): List<Long> {
    val translationEntities = event.modifiedEntities[Translation::class] ?: return emptyList()
    return translationEntities
      .filter { (_, entity) ->
        entity.revisionType != RevisionType.DEL &&
          entity.modifications.containsKey("text")
      }.keys
      .toList()
  }

  private fun handleLanguageTagChanged(
    event: OnProjectActivityEvent,
    projectId: Long,
  ) {
    val languageEntities = event.modifiedEntities[Language::class] ?: return
    val changedLanguageIds =
      languageEntities
        .filter { (_, entity) -> entity.modifications.containsKey("tag") }
        .map { (id, _) -> id }

    if (changedLanguageIds.isEmpty()) return

    qaRecheckService.recheckTranslations(
      projectId = projectId,
      languageIds = changedLanguageIds,
    )
  }

  private fun handleBaseLanguageChanged(
    event: OnProjectActivityEvent,
    projectId: Long,
  ) {
    val projectEntities = event.modifiedEntities[Project::class] ?: return
    val baseLanguageChanged =
      projectEntities.values.any { entity ->
        entity.modifications.containsKey("baseLanguage")
      }

    if (!baseLanguageChanged) return

    qaRecheckService.recheckTranslations(projectId = projectId)
  }

  /**
   * Enables QA checks for new projects if the feature is available.
   */
  private fun handleProjectCreated(
    event: OnProjectActivityEvent,
    projectId: Long,
  ) {
    val modifiedProjects = event.modifiedEntities[Project::class] ?: return
    val isNewProject = modifiedProjects.values.any { it.revisionType == RevisionType.ADD }
    if (!isNewProject) return

    val project = entityManager.find(Project::class.java, projectId) ?: return
    val orgId = project.organizationOwner.id
    if (enabledFeaturesProvider.isFeatureEnabled(orgId, Feature.QA_CHECKS)) {
      project.useQaChecks = true
    }
  }

  private fun handleBatchJobCompleted(event: OnBatchJobCompleted) {
    if (event.job.type !in TRANSLATION_MODIFYING_BATCH_TYPES) return

    val projectId = event.job.projectId ?: return

    runSentryCatching {
      val project = projectService.findDto(projectId) ?: return@runSentryCatching
      if (!projectFeatureGuard.isFeatureEnabled(Feature.QA_CHECKS, project)) return@runSentryCatching

      qaRecheckService.recheckTranslations(
        projectId = projectId,
        onlyStale = true,
      )
    }
  }

  companion object {
    private val TRANSLATION_MODIFYING_BATCH_TYPES =
      setOf(
        BatchJobType.MACHINE_TRANSLATE,
        BatchJobType.AUTO_TRANSLATE,
        BatchJobType.COPY_TRANSLATIONS,
        BatchJobType.CLEAR_TRANSLATIONS,
        BatchJobType.PRE_TRANSLATE_BT_TM,
      )
  }
}
