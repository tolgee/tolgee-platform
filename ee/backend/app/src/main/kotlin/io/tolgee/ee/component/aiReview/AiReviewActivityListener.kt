package io.tolgee.ee.component.aiReview

import io.tolgee.activity.ActivityService
import io.tolgee.activity.data.EntityDescription
import io.tolgee.activity.data.EntityDescriptionRef
import io.tolgee.activity.data.PropertyModification
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
import io.tolgee.repository.KeyMetaRepository
import io.tolgee.repository.TranslationRepository
import io.tolgee.repository.aiReview.TranslationAiReviewRepository
import io.tolgee.service.language.LanguageService
import io.tolgee.service.project.ProjectFeatureGuard
import io.tolgee.service.project.ProjectService
import io.tolgee.util.runSentryCatching
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
  private val reviewRepository: TranslationAiReviewRepository,
  private val objectMapper: ObjectMapper,
  private val keyMetaRepository: KeyMetaRepository,
  private val aiReviewActionService: AiReviewActionService,
  private val aiReviewRunService: AiReviewRunService,
) : BypassableActivityListener {
  @Volatile
  override var bypass = false

  @EventListener
  fun onActivity(event: OnProjectActivityEvent) {
    if (bypass) return
    runSentryCatching {
      val revision = event.activityRevision
      val projectId = revision.projectId ?: return
      val entities = event.modifiedEntities.values.flatMap { it.values }
      if (revision.batchJobChunkExecution != null) {
        applyHumanStateChanges(projectId, entities, revision.systemActor, revision.authorId)
        return
      }

      handleBaseLanguageChanged(event, projectId)
      applyHumanStateChanges(projectId, entities, revision.systemActor, revision.authorId)
      applyModifiedEntities(projectId, entities)
    }
  }

  @EventListener
  fun onBatchJobFinalized(event: OnBatchJobFinalized) {
    if (bypass) return
    runSentryCatching {
      val projectId = event.job.projectId ?: return
      when (event.job.type) {
        BatchJobType.AI_REVIEW -> Unit
        BatchJobType.QA_CHECK -> startReviewsReadyAfterQa(event, projectId)
        else -> applyActivityOfJob(event, projectId)
      }
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

  private fun applyActivityOfJob(
    event: OnBatchJobFinalized,
    projectId: Long,
  ) {
    val activityRevisionId = event.activityRevisionId ?: return
    if (!isAiReviewOn(projectId) && !reviewRepository.existsOnKeysOfRevisionTranslations(activityRevisionId)) return
    applyModifiedEntities(projectId, activityService.findModifiedEntitiesByRevisionId(activityRevisionId))
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

  private fun startReviewsReadyAfterQa(
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

  private fun applyModifiedEntities(
    projectId: Long,
    entities: List<ActivityModifiedEntity>,
  ) {
    startDemotionForKeyScopeChanges(projectId, entities)
    val textChanged = textChangedTranslations(entities)
    if (textChanged.isEmpty()) return
    // README 3.4: a text change deletes rows even while the feature is off.
    if (!isAiReviewOn(projectId) && !hasRowsOnKeysOf(textChanged)) return

    val baseLanguageId = languageService.getProjectBaseLanguage(projectId).id
    aiReviewQueueService.onSourceChanged(projectId, baseTextKeyIds(textChanged, baseLanguageId))
    aiReviewQueueService.onTextChanged(projectId, textChanged.map { it.entityId })
  }

  private fun applyHumanStateChanges(
    projectId: Long,
    entities: List<ActivityModifiedEntity>,
    systemActor: SystemActor?,
    authorId: Long?,
  ) {
    if (systemActor != null) return
    // Also while the feature is off: a kept row would make the human review AI-managed again
    // when the feature comes back.
    val reviewedIds = entities.filter { isReview(it) }.map { it.entityId }
    if (reviewedIds.isNotEmpty()) aiReviewActionService.releaseToHumanReview(projectId, reviewedIds)
    val unReviews = entities.filter { isUnReview(it) }
    if (unReviews.isEmpty()) return
    aiReviewActionService.settleHumanUnReview(
      projectId,
      unReviews.map { it.entityId },
      unReviews.filter { it.modifications.containsKey("text") }.map { it.entityId }.toSet(),
      authorId,
      isAiReviewOn(projectId),
    )
  }

  private fun isUnReview(entity: ActivityModifiedEntity): Boolean {
    val stateChange = translationStateChange(entity) ?: return false
    return stateChange.old?.toString() == TranslationState.REVIEWED.name &&
      stateChange.new?.toString() == TranslationState.TRANSLATED.name
  }

  private fun isReview(entity: ActivityModifiedEntity): Boolean {
    val stateChange = translationStateChange(entity) ?: return false
    return stateChange.old?.toString() != TranslationState.REVIEWED.name &&
      stateChange.new?.toString() == TranslationState.REVIEWED.name
  }

  private fun translationStateChange(entity: ActivityModifiedEntity): PropertyModification? {
    if (entity.entityClass != Translation::class.simpleName) return null
    if (entity.revisionType != RevisionType.MOD) return null
    return entity.modifications["state"]
  }

  private fun startDemotionForKeyScopeChanges(
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
    if (namespaceChangedKeyIds.isEmpty() && tagsChangedKeyMetaIds.isEmpty()) return
    if (!isAiReviewOn(projectId)) return
    val keyIds =
      namespaceChangedKeyIds +
        tagsChangedKeyMetaIds.chunked(IN_CLAUSE_CHUNK).flatMap { keyMetaRepository.findKeyIdsByIds(it) }
    if (keyIds.isEmpty()) return
    aiReviewQueueService.startDemotionJob(projectId, keyIds.distinct())
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

  private fun hasRowsOnKeysOf(textChanged: List<ActivityModifiedEntity>): Boolean =
    textChanged
      .map { it.entityId }
      .chunked(IN_CLAUSE_CHUNK)
      .any { reviewRepository.existsOnKeysOfTranslations(it) }

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
