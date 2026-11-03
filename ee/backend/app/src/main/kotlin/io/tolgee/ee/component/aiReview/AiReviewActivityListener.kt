package io.tolgee.ee.component.aiReview

import io.tolgee.activity.ActivityService
import io.tolgee.activity.data.EntityDescription
import io.tolgee.activity.data.EntityDescriptionRef
import io.tolgee.activity.data.RevisionType
import io.tolgee.batch.data.BatchJobDto
import io.tolgee.batch.data.BatchJobType
import io.tolgee.batch.data.BatchTranslationTargetItem
import io.tolgee.batch.events.OnBatchJobCancelled
import io.tolgee.batch.events.OnBatchJobFinalized
import io.tolgee.component.eventListeners.BypassableActivityListener
import io.tolgee.constants.Feature
import io.tolgee.ee.service.aiReview.AiReviewActionService
import io.tolgee.ee.service.aiReview.AiReviewQueueService
import io.tolgee.ee.service.aiReview.AiReviewRunService
import io.tolgee.ee.service.aiReview.IN_CLAUSE_CHUNK
import io.tolgee.events.OnProjectActivityEvent
import io.tolgee.events.OnQaIssuesIgnored
import io.tolgee.model.Project
import io.tolgee.model.activity.ActivityModifiedEntity
import io.tolgee.model.batch.params.AiReviewJobParams
import io.tolgee.model.batch.params.QaCheckJobParams
import io.tolgee.model.enums.SystemActor
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.key.Key
import io.tolgee.model.key.KeyMeta
import io.tolgee.model.translation.Translation
import io.tolgee.repository.TranslationRepository
import io.tolgee.service.language.LanguageService
import io.tolgee.service.project.ProjectFeatureGuard
import io.tolgee.service.project.ProjectService
import io.tolgee.util.runSentryCatching
import jakarta.persistence.EntityManager
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

@Component
class AiReviewActivityListener(
  private val activityService: ActivityService,
  private val aiReviewQueueService: AiReviewQueueService,
  private val languageService: LanguageService,
  private val projectService: ProjectService,
  private val projectFeatureGuard: ProjectFeatureGuard,
  private val translationRepository: TranslationRepository,
  private val objectMapper: ObjectMapper,
  private val entityManager: EntityManager,
  private val aiReviewActionService: AiReviewActionService,
  private val aiReviewRunService: AiReviewRunService,
) : BypassableActivityListener {
  @Volatile
  override var bypass = false

  @EventListener
  fun onActivity(event: OnProjectActivityEvent) {
    if (bypass) return
    runSentryCatching {
      val projectId = event.activityRevision.projectId ?: return
      if (event.activityRevision.batchJobChunkExecution != null) return

      handleBaseLanguageChanged(event, projectId)
      applyTranslationChanges(
        projectId,
        event.modifiedEntities.values.flatMap { it.values },
        systemActor = event.activityRevision.systemActor,
        authorId = event.activityRevision.authorId,
      )
    }
  }

  @EventListener
  fun onBatchJobFinalized(event: OnBatchJobFinalized) {
    if (bypass) return
    runSentryCatching {
      val projectId = event.job.projectId ?: return
      handleFinalizedActivity(event, projectId)
      selfHealAfterJob(event, projectId)
    }
  }

  @EventListener
  fun onQaIssuesIgnored(event: OnQaIssuesIgnored) {
    if (bypass) return
    runSentryCatching {
      aiReviewQueueService.onQaIssuesIgnored(event.projectId, event.translationIds)
    }
  }

  @EventListener
  fun onBatchJobCancelled(event: OnBatchJobCancelled) {
    if (bypass) return
    if (event.job.type != BatchJobType.AI_REVIEW) return
    runSentryCatching {
      aiReviewRunService.onRunCancelled(event.job.id)
    }
  }

  private fun handleFinalizedActivity(
    event: OnBatchJobFinalized,
    projectId: Long,
  ) {
    if (event.job.type == BatchJobType.AI_REVIEW) return
    if (event.job.type == BatchJobType.QA_CHECK) {
      handleQaFinalized(event, projectId)
      return
    }

    val activityRevisionId = event.activityRevisionId ?: return
    val entities = activityService.findModifiedEntitiesByRevisionId(activityRevisionId)
    applyTranslationChanges(
      projectId,
      entities,
      systemActor = event.job.type.systemActor,
      authorId = event.job.authorId,
    )
  }

  private fun selfHealAfterJob(
    event: OnBatchJobFinalized,
    projectId: Long,
  ) {
    if (event.job.type != BatchJobType.QA_CHECK && event.job.type != BatchJobType.AI_REVIEW) return
    if (isSelfHealJob(event.job)) return
    aiReviewQueueService.selfHeal(projectId, event.job.id)
  }

  private fun isSelfHealJob(job: BatchJobDto): Boolean {
    val params = job.params ?: return false
    if (job.type == BatchJobType.AI_REVIEW) {
      return objectMapper.convertValue(params, AiReviewJobParams::class.java).handlingStuckItems
    }
    return objectMapper.convertValue(params, QaCheckJobParams::class.java).handlingStuckStaleItems
  }

  private fun handleQaFinalized(
    event: OnBatchJobFinalized,
    projectId: Long,
  ) {
    if (!isAiReviewOn(projectId)) return
    val items = event.job.target.map { objectMapper.convertValue(it, BatchTranslationTargetItem::class.java) }
    if (items.isEmpty()) return
    val translationIds =
      items.map { it.keyId }.distinct().chunked(IN_CLAUSE_CHUNK).flatMap { keyIds ->
        translationRepository.getIdsByKeyIdsAndLanguageIds(keyIds, items.map { it.languageId }.distinct())
      }
    aiReviewQueueService.startJobsForReady(projectId, translationIds)
  }

  private fun applyTranslationChanges(
    projectId: Long,
    entities: List<ActivityModifiedEntity>,
    systemActor: SystemActor?,
    authorId: Long?,
  ) {
    if (isAiReviewOn(projectId)) handleHumanUnReview(entities, systemActor, authorId)
    handleKeyScopeChanged(projectId, entities)
    val textChanged = textChangedTranslations(entities)
    if (textChanged.isEmpty()) return

    val baseLanguageId = languageService.getProjectBaseLanguage(projectId).id
    aiReviewQueueService.onSourceChanged(projectId, baseTextKeyIds(textChanged, baseLanguageId))
    aiReviewQueueService.onTextChanged(projectId, textChanged.map { it.entityId })
  }

  private fun handleHumanUnReview(
    entities: List<ActivityModifiedEntity>,
    systemActor: SystemActor?,
    authorId: Long?,
  ) {
    if (systemActor != null) return
    val unReviewedIds = entities.filter { isHumanUnReview(it) }.map { it.entityId }
    if (unReviewedIds.isEmpty()) return
    aiReviewActionService.markRejectedByStateChange(unReviewedIds, authorId)
  }

  private fun isHumanUnReview(entity: ActivityModifiedEntity): Boolean {
    if (entity.entityClass != Translation::class.simpleName) return false
    if (entity.revisionType != RevisionType.MOD) return false
    val stateChange = entity.modifications["state"] ?: return false
    return stateChange.old?.toString() == TranslationState.REVIEWED.name &&
      stateChange.new?.toString() == TranslationState.TRANSLATED.name
  }

  private fun handleKeyScopeChanged(
    projectId: Long,
    entities: List<ActivityModifiedEntity>,
  ) {
    val namespaceChangedKeyIds =
      entities
        .filter {
          it.entityClass == Key::class.simpleName &&
            it.revisionType == RevisionType.MOD &&
            it.modifications.containsKey("namespace")
        }.map { it.entityId }
    val tagsChangedKeyMetaIds =
      entities
        .filter {
          it.entityClass == KeyMeta::class.simpleName &&
            it.revisionType != RevisionType.DEL &&
            it.modifications.containsKey("tags")
        }.map { it.entityId }
    val keyIds = namespaceChangedKeyIds + keyIdsOfKeyMetas(tagsChangedKeyMetaIds)
    if (keyIds.isEmpty()) return
    aiReviewQueueService.startDemotionJob(projectId, keyIds.distinct())
  }

  private fun keyIdsOfKeyMetas(keyMetaIds: List<Long>): List<Long> {
    if (keyMetaIds.isEmpty()) return emptyList()
    return keyMetaIds.chunked(IN_CLAUSE_CHUNK).flatMap { chunk ->
      entityManager
        .createNativeQuery("select key_id from key_meta where id in :ids")
        .setParameter("ids", chunk)
        .resultList
        .map { (it as Number).toLong() }
    }
  }

  /**
   * The id comes from the modification itself: this event runs in beforeTransactionCompletion,
   * where a languageService lookup can write (handleMissingBaseLanguage) or fail during project
   * creation and deletion.
   */
  private fun handleBaseLanguageChanged(
    event: OnProjectActivityEvent,
    projectId: Long,
  ) {
    val projectEntities = event.modifiedEntities[Project::class] ?: return
    val baseLanguageChanged =
      projectEntities.values
        .filter { it.revisionType == RevisionType.MOD }
        .any { entityIdOf(it.modifications["baseLanguage"]?.new) != null }
    if (!baseLanguageChanged) return
    aiReviewQueueService.onBaseLanguageChanged(projectId)
  }

  private fun baseTextKeyIds(
    textChanged: List<ActivityModifiedEntity>,
    baseLanguageId: Long,
  ): List<Long> =
    textChanged
      .filter { it.describingRelations?.get("language")?.entityId == baseLanguageId }
      .mapNotNull { it.describingRelations?.get("key")?.entityId }

  private fun textChangedTranslations(entities: List<ActivityModifiedEntity>): List<ActivityModifiedEntity> =
    entities.filter {
      it.entityClass == Translation::class.simpleName &&
        it.revisionType != RevisionType.DEL &&
        it.modifications.containsKey("text")
    }

  private fun isAiReviewOn(projectId: Long): Boolean {
    // The activity can describe a project hard-delete, so the project may be gone already.
    val project = projectService.findDto(projectId) ?: return false
    return projectFeatureGuard.isFeatureEnabled(Feature.AI_REVIEW, project)
  }

  private fun entityIdOf(value: Any?): Long? {
    if (value is EntityDescription) return value.entityId
    if (value is EntityDescriptionRef) return value.entityId
    if (value is Map<*, *>) return (value["entityId"] as? Number)?.toLong()
    return null
  }
}
