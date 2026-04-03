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
 * - [OnProjectActivityEvent]: entity modifications detected by Hibernate (translation text
 *   changes, project creation, language tag changes, base language changes)
 * - [OnBatchJobCompleted]: batch job completions for bulk translation text modifications
 *
 * Changes are detected via the activity Hibernate interceptor.
 * Batch job chunk activities are skipped — handled in bulk when the batch job completes.
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

      // Skip batch job chunk activities — handled by onBatchJobCompleted
      if (event.activityRevision.batchJobChunkExecution != null) return

      val modifiedEntityClasses = event.modifiedEntities.keys.toSet()

      if (Project::class in modifiedEntityClasses) {
        val isNewProject =
          event.modifiedEntities[Project::class]?.values?.any {
            it.revisionType == RevisionType.ADD
          } == true
        if (isNewProject) {
          handleProjectCreated(projectId)
        }
      }

      val project = projectService.findDto(projectId) ?: return
      if (!projectFeatureGuard.isFeatureEnabled(Feature.QA_CHECKS, project)) return

      handleLanguageTagChanged(event, projectId)
      handleBaseLanguageChanged(event, projectId)
      handleTranslationTextChanges(event, projectId)
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

  private fun handleTranslationTextChanges(
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

    // If base text changes, mark translations for all other languages as stale
    val baseLanguage = languageService.getProjectBaseLanguage(projectId)
    val textChangedTranslationIds = textChangedEntities.keys.toList()

    val siblingIds =
      translationService.getSiblingIdsForBaseLanguageChanges(
        translationIds = textChangedTranslationIds,
        baseLanguageId = baseLanguage.id,
      )
    if (siblingIds.isNotEmpty()) {
      translationService.setQaChecksStale(siblingIds)
    }

    // For base text changes, we also include all project languages
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

  private fun handleProjectCreated(projectId: Long) {
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
