package io.tolgee.ee.component.qa

import io.tolgee.activity.ActivityService
import io.tolgee.activity.data.RevisionType
import io.tolgee.batch.BatchJobService
import io.tolgee.batch.data.BatchJobType
import io.tolgee.batch.data.BatchTranslationTargetItem
import io.tolgee.batch.events.OnBatchJobFinalized
import io.tolgee.batch.processors.QaCheckChunkProcessor
import io.tolgee.batch.request.QaCheckRequest
import io.tolgee.component.enabledFeaturesProvider.EnabledFeaturesProvider
import io.tolgee.component.eventListeners.BypassableActivityListener
import io.tolgee.constants.Feature
import io.tolgee.ee.service.qa.ProjectQaConfigService
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
  private val qaCheckChunkProcessor: QaCheckChunkProcessor,
  private val projectQaConfigService: ProjectQaConfigService,
) : BypassableActivityListener {
  @Volatile
  override var bypass = false

  // region Event listeners

  @EventListener
  fun onActivity(event: OnProjectActivityEvent) {
    if (bypass) return
    runSentryCatching {
      val projectId = event.activityRevision.projectId ?: return
      val isBatchChunk = event.activityRevision.batchJobChunkExecution != null

      // Mark translations stale unconditionally - even when QA is disabled and even when processing batch chunks
      markTranslationsQaChecksStale(event, projectId)
      markMaxCharLimitTranslationsQaChecksStale(event)
      markLanguageTagChangedTranslationsStale(event, projectId)
      markBaseLanguageChangedTranslationsStale(event, projectId)

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
    if (bypass) return
    runSentryCatching {
      val projectId = event.job.projectId ?: return

      val project = projectService.findDto(projectId) ?: return@runSentryCatching
      if (!projectFeatureGuard.isFeatureEnabled(Feature.QA_CHECKS, project)) return@runSentryCatching

      val entities = activityService.findModifiedEntitiesByRevisionId(event.activityRevisionId)
      if (entities.isEmpty()) return@runSentryCatching

      processModifiedEntities(projectId, entities)
    }
  }

  @EventListener
  fun onQaBatchJobFinalizedSelfHeal(event: OnBatchJobFinalized) {
    if (bypass) return
    if (event.job.type != BatchJobType.QA_CHECK) return
    val projectId = event.job.projectId ?: return
    val params = qaCheckChunkProcessor.getParams(event.job)
    if (params.handlingStuckStaleItems) {
      // Avoid endless loop
      return
    }
    runSentryCatching {
      qaRecheckService.recheckStuckStaleTranslationsInProject(projectId)
    }
  }

  // endregion

  // region Batch job processing (feature-gated)

  private fun processModifiedEntities(
    projectId: Long,
    entities: List<ActivityModifiedEntity>,
  ) {
    val enabledLanguageIds = projectQaConfigService.getEnabledLanguageIds(projectId)

    val translationTargets = getTranslationChangeTargets(projectId, entities, enabledLanguageIds)
    val maxCharLimitTargets = getMaxCharLimitChangeTargets(entities, enabledLanguageIds)

    val allTargets = (translationTargets + maxCharLimitTargets).distinct()
    startQaCheckBatchJob(projectId, allTargets)
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

  // endregion

  // region Translation text changes

  private fun markTranslationsQaChecksStale(
    event: OnProjectActivityEvent,
    projectId: Long,
  ) {
    val textChangedEntities =
      getTextChangedTranslations(event.modifiedEntities.values.flatMap { it.values })
    if (textChangedEntities.isEmpty()) return

    translationService.setQaChecksStale(textChangedEntities.map { it.entityId })

    // Mark siblings of base-language changes as stale.
    val baseLanguage = languageService.getProjectBaseLanguage(projectId)
    val baseLanguageTranslationIds =
      textChangedEntities
        .filter { it.describingRelations?.get("language")?.entityId == baseLanguage.id }
        .map { it.entityId }
    if (baseLanguageTranslationIds.isNotEmpty()) {
      translationService.setQaChecksStaleForBaseTranslationKeys(
        translationIds = baseLanguageTranslationIds.toLongArray(),
        baseLanguageId = baseLanguage.id,
      )
    }
  }

  private fun getTranslationChangeTargets(
    projectId: Long,
    entities: List<ActivityModifiedEntity>,
    enabledLanguageIds: Set<Long>,
  ): List<BatchTranslationTargetItem> {
    val textChangedEntities = getTextChangedTranslations(entities)
    if (textChangedEntities.isEmpty()) return emptyList()

    // Extract direct targets from describing relations
    val directTargets =
      textChangedEntities.mapNotNull { entity ->
        val keyId = entity.describingRelations?.get("key")?.entityId ?: return@mapNotNull null
        val languageId = entity.describingRelations?.get("language")?.entityId ?: return@mapNotNull null
        if (languageId !in enabledLanguageIds) return@mapNotNull null
        BatchTranslationTargetItem(keyId = keyId, languageId = languageId)
      }

    // For base text changes, also include all project languages
    val baseLanguage = languageService.getProjectBaseLanguage(projectId)
    val baseLanguageKeyIds =
      textChangedEntities
        .filter { it.describingRelations?.get("language")?.entityId == baseLanguage.id }
        .mapNotNull { it.describingRelations?.get("key")?.entityId }
        .toSet()

    val siblingTargets =
      if (baseLanguageKeyIds.isNotEmpty()) {
        val otherLanguageIds = enabledLanguageIds.filter { it != baseLanguage.id }
        otherLanguageIds.flatMap { langId ->
          baseLanguageKeyIds.map { keyId ->
            BatchTranslationTargetItem(keyId = keyId, languageId = langId)
          }
        }
      } else {
        emptyList()
      }

    return directTargets + siblingTargets
  }

  private fun getTextChangedTranslations(entities: List<ActivityModifiedEntity>): List<ActivityModifiedEntity> {
    return entities.filter { entity ->
      entity.entityClass == Translation::class.simpleName &&
        entity.revisionType != RevisionType.DEL &&
        entity.modifications.containsKey("text")
    }
  }

  // endregion

  // region Max char limit changes

  private fun markMaxCharLimitTranslationsQaChecksStale(event: OnProjectActivityEvent) {
    val keyIds = getMaxCharLimitChangedKeyIds(event.modifiedEntities.values.flatMap { it.values })
    if (keyIds.isEmpty()) return
    translationService.setQaChecksStaleByKeyIds(keyIds)
  }

  private fun getMaxCharLimitChangeTargets(
    entities: List<ActivityModifiedEntity>,
    enabledLanguageIds: Set<Long>,
  ): List<BatchTranslationTargetItem> {
    val keyIds = getMaxCharLimitChangedKeyIds(entities)
    if (keyIds.isEmpty()) return emptyList()

    return enabledLanguageIds.flatMap { langId ->
      keyIds.map { keyId ->
        BatchTranslationTargetItem(keyId = keyId, languageId = langId)
      }
    }
  }

  private fun getMaxCharLimitChangedKeyIds(entities: List<ActivityModifiedEntity>): List<Long> {
    return entities
      .filter { entity ->
        entity.entityClass == Key::class.simpleName &&
          entity.revisionType != RevisionType.DEL &&
          entity.modifications.containsKey("maxCharLimit")
      }.map { it.entityId }
  }

  // endregion

  // region Language tag changes

  private fun markLanguageTagChangedTranslationsStale(
    event: OnProjectActivityEvent,
    projectId: Long,
  ) {
    val changedLanguageIds = getLanguageTagChangedIds(event)
    if (changedLanguageIds.isEmpty()) return

    val baseLanguageId = languageService.getProjectBaseLanguage(projectId).id
    if (baseLanguageId in changedLanguageIds) {
      // Base language tag changed — all QA results reference baseLanguageTag, so invalidate project-wide
      translationService.setQaChecksStaleByProjectId(projectId)
      return
    }

    translationService.setQaChecksStaleByProjectIdAndLanguageIds(projectId, changedLanguageIds)
  }

  private fun handleLanguageTagChanged(
    event: OnProjectActivityEvent,
    projectId: Long,
  ) {
    val changedLanguageIds = getLanguageTagChangedIds(event)
    if (changedLanguageIds.isEmpty()) return

    val baseLanguageId = languageService.getProjectBaseLanguage(projectId).id
    if (baseLanguageId in changedLanguageIds) {
      // Base language tag changed — recheck all translations project-wide
      qaRecheckService.recheckTranslations(projectId = projectId)
      return
    }

    qaRecheckService.recheckTranslations(
      projectId = projectId,
      languageIds = changedLanguageIds,
    )
  }

  private fun getLanguageTagChangedIds(event: OnProjectActivityEvent): List<Long> {
    val languageEntities = event.modifiedEntities[Language::class] ?: return emptyList()
    return languageEntities
      .filter { (_, entity) -> entity.modifications.containsKey("tag") }
      .map { (id, _) -> id }
  }

  // endregion

  // region Base language changes

  private fun markBaseLanguageChangedTranslationsStale(
    event: OnProjectActivityEvent,
    projectId: Long,
  ) {
    if (!isBaseLanguageChanged(event)) return
    translationService.setQaChecksStaleByProjectId(projectId)
  }

  private fun handleBaseLanguageChanged(
    event: OnProjectActivityEvent,
    projectId: Long,
  ) {
    if (!isBaseLanguageChanged(event)) return
    qaRecheckService.recheckTranslations(projectId = projectId)
  }

  private fun isBaseLanguageChanged(event: OnProjectActivityEvent): Boolean {
    val projectEntities = event.modifiedEntities[Project::class] ?: return false
    return projectEntities.values.any { entity ->
      entity.modifications.containsKey("baseLanguage")
    }
  }

  // endregion

  // region Project creation

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

  // endregion
}
