package io.tolgee.ee.component.qa

import io.tolgee.activity.ActivityService
import io.tolgee.activity.data.RevisionType
import io.tolgee.batch.BatchJobService
import io.tolgee.batch.data.BatchJobType
import io.tolgee.batch.data.BatchTranslationTargetItem
import io.tolgee.batch.events.OnBatchJobFinalized
import io.tolgee.batch.request.QaCheckRequest
import io.tolgee.component.enabledFeaturesProvider.EnabledFeaturesProvider
import io.tolgee.constants.Feature
import io.tolgee.ee.service.qa.QaRecheckService
import io.tolgee.events.OnProjectActivityEvent
import io.tolgee.model.Language
import io.tolgee.model.Project
import io.tolgee.model.activity.ActivityModifiedEntity
import io.tolgee.model.key.Key
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
 * - [OnProjectActivityEvent]: entity modifications (translation text changes, key maxCharLimit
 *   changes, project creation, language tag changes, base language changes)
 * - [OnBatchJobFinalized]: batch job completions — loads the merged activity
 *
 * Stale marking (qaChecksStale = true) runs unconditionally — even when the QA feature is disabled.
 * This is needed so translations have the correct stale state when QA is enabled later
 * and when batch jobs complete.
 */
@Component
class QaActivityListener(
  private val activityService: ActivityService,
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
      markTranslationsQaChecksStale(event, projectId)
      markMaxCharLimitTranslationsQaChecksStale(event)

      // If this activity is part of a batch chunk, we'll handle it when the batch job is finalized
      if (isBatchChunk) return

      // Enable QA checks for new projects if the feature is available
      handleProjectCreated(event, projectId)

      val project = projectService.findDto(projectId) ?: return
      if (!projectFeatureGuard.isFeatureEnabled(Feature.QA_CHECKS, project)) return

      handleLanguageTagChanged(event, projectId)
      handleBaseLanguageChanged(event, projectId)

      val allEntities = event.modifiedEntities.values.flatMap { it.values }
      processModifiedEntities(projectId, allEntities)
    }
  }

  @EventListener
  fun onBatchJobFinalized(event: OnBatchJobFinalized) {
    runSentryCatching {
      val projectId = event.job.projectId ?: return

      val project = projectService.findDto(projectId) ?: return@runSentryCatching
      if (!projectFeatureGuard.isFeatureEnabled(Feature.QA_CHECKS, project)) return@runSentryCatching

      val entities = activityService.findModifiedEntitiesByRevisionId(event.activityRevisionId)
      if (entities.isEmpty()) return@runSentryCatching

      processModifiedEntities(projectId, entities)
    }
  }

  private fun processModifiedEntities(
    projectId: Long,
    entities: List<ActivityModifiedEntity>,
  ) {
    val translationTargets = getTranslationChangeTargets(projectId, entities)
    val maxCharLimitTargets = getMaxCharLimitChangeTargets(projectId, entities)

    val allTargets = (translationTargets + maxCharLimitTargets).distinct()
    startQaCheckBatchJob(projectId, allTargets)
  }

  private fun getTranslationChangeTargets(
    projectId: Long,
    entities: List<ActivityModifiedEntity>,
  ): List<BatchTranslationTargetItem> {
    val textChangedEntities = getTextChangedTranslations(entities)
    if (textChangedEntities.isEmpty()) return emptyList()

    // Extract direct targets from describing relations
    val directTargets =
      textChangedEntities.mapNotNull { entity ->
        val keyId = entity.describingRelations?.get("key")?.entityId ?: return@mapNotNull null
        val languageId = entity.describingRelations?.get("language")?.entityId ?: return@mapNotNull null
        BatchTranslationTargetItem(keyId = keyId, languageId = languageId)
      }

    if (directTargets.isEmpty()) return emptyList()

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

    return directTargets + siblingTargets
  }

  private fun getMaxCharLimitChangeTargets(
    projectId: Long,
    entities: List<ActivityModifiedEntity>,
  ): List<BatchTranslationTargetItem> {
    val keyIds = getMaxCharLimitChangedKeyIds(entities)
    if (keyIds.isEmpty()) return emptyList()

    val allLanguageIds = languageService.getProjectLanguages(projectId).map { it.id }
    return keyIds.flatMap { keyId ->
      allLanguageIds.map { langId -> BatchTranslationTargetItem(keyId = keyId, languageId = langId) }
    }
  }

  private fun markTranslationsQaChecksStale(
    event: OnProjectActivityEvent,
    projectId: Long,
  ) {
    val textChangedTranslationIds =
      getTextChangedTranslations(event.modifiedEntities.values.flatMap { it.values }).map { it.entityId }
    if (textChangedTranslationIds.isEmpty()) return

    // Mark direct translations as stale
    translationService.setQaChecksStale(textChangedTranslationIds)

    // Mark siblings of base language changes as stale
    val baseLanguage = languageService.getProjectBaseLanguage(projectId)
    translationService.setQaChecksStaleForBaseTranslationKeys(
      translationIds = textChangedTranslationIds,
      baseLanguageId = baseLanguage.id,
    )
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

  private fun markMaxCharLimitTranslationsQaChecksStale(event: OnProjectActivityEvent) {
    val keyIds = getMaxCharLimitChangedKeyIds(event.modifiedEntities.values.flatMap { it.values })
    if (keyIds.isEmpty()) return
    translationService.setQaChecksStaleByKeyIds(keyIds)
  }

  private fun getMaxCharLimitChangedKeyIds(entities: List<ActivityModifiedEntity>): List<Long> {
    return entities
      .filter { entity ->
        entity.entityClass == Key::class.simpleName &&
          entity.revisionType != RevisionType.DEL &&
          entity.modifications.containsKey("maxCharLimit")
      }.map { it.entityId }
  }

  private fun getTextChangedTranslations(entities: List<ActivityModifiedEntity>): List<ActivityModifiedEntity> {
    return entities.filter { entity ->
      entity.entityClass == Translation::class.simpleName &&
        entity.revisionType != RevisionType.DEL &&
        entity.modifications.containsKey("text")
    }
  }

  private fun startQaCheckBatchJob(
    projectId: Long,
    targets: List<BatchTranslationTargetItem>,
  ) {
    if (targets.isEmpty()) return
    val project = entityManager.getReference(Project::class.java, projectId)
    for (chunk in targets.chunked(QaRecheckService.MAX_BATCH_JOB_TARGET_SIZE)) {
      batchJobService.startJob(
        request = QaCheckRequest(target = chunk),
        project = project,
        author = null,
        type = BatchJobType.QA_CHECK,
        isHidden = true,
      )
    }
  }
}
