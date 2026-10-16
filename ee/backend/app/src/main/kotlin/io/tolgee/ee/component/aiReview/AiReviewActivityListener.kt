package io.tolgee.ee.component.aiReview

import io.tolgee.activity.ActivityService
import io.tolgee.activity.data.ActivityType
import io.tolgee.activity.data.RevisionType
import io.tolgee.batch.data.BatchJobType
import io.tolgee.batch.data.BatchTranslationTargetItem
import io.tolgee.batch.events.OnBatchJobFinalized
import io.tolgee.batch.processors.AiReviewChunkProcessor
import io.tolgee.component.eventListeners.BypassableActivityListener
import io.tolgee.dtos.cacheable.ProjectDto
import io.tolgee.ee.service.aiReview.AiReviewTriggerService
import io.tolgee.events.OnProjectActivityEvent
import io.tolgee.model.activity.ActivityModifiedEntity
import io.tolgee.model.batch.BatchJobStatus
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.key.Key
import io.tolgee.model.key.KeyMeta
import io.tolgee.model.translation.Translation
import io.tolgee.repository.aiReview.AiReviewRuleRepository
import io.tolgee.service.aiReview.AiReviewAvailability
import io.tolgee.service.project.ProjectService
import io.tolgee.util.runSentryCatching
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

@Component
class AiReviewActivityListener(
  private val triggerService: AiReviewTriggerService,
  private val projectService: ProjectService,
  private val aiReviewAvailability: AiReviewAvailability,
  private val aiReviewRuleRepository: AiReviewRuleRepository,
  private val activityService: ActivityService,
  private val objectMapper: ObjectMapper,
  private val aiReviewChunkProcessor: AiReviewChunkProcessor,
) : BypassableActivityListener {
  @Volatile
  override var bypass = false

  @EventListener
  fun onActivity(event: OnProjectActivityEvent) {
    if (bypass) return
    runSentryCatching {
      val projectId = event.activityRevision.projectId ?: return
      val entities = event.modifiedEntities.values.flatMap { it.values }

      val textChangedEntities = getTextChangedTranslations(entities)
      val textChanges = textChangedEntities.mapNotNull { it.toTextChange() }
      triggerService.invalidate(textChangedEntities.map { it.entityId })
      val sourceChangedSiblings = triggerService.resetSiblingsOfChangedSources(projectId, textChanges)
      recordRejectionsFromManualStateChanges(event, entities)

      val project = findProjectWithActiveAiReview(projectId) ?: return
      val targets = triggerService.createPending(projectId, textChanges + sourceChangedSiblings)

      if (event.activityRevision.batchJobChunkExecution != null) return

      triggerService.startReviewIfNotQaCovered(projectId, project, targets)
      triggerService.startReevaluation(projectId, getScopeChangedKeyIds(entities))
    }
  }

  @EventListener
  fun onBatchJobFinalized(event: OnBatchJobFinalized) {
    if (bypass) return
    runSentryCatching {
      val projectId = event.job.projectId ?: return
      abandonPendingOfCancelledRun(projectId, event)
      val project = findProjectWithActiveAiReview(projectId) ?: return

      when (event.job.type) {
        BatchJobType.AI_REVIEW -> onAiReviewJobFinalized(projectId, project, event)
        BatchJobType.QA_CHECK -> onQaJobFinalized(projectId, project, event)
        else -> onOtherJobFinalized(projectId, project, event)
      }
    }
  }

  private fun abandonPendingOfCancelledRun(
    projectId: Long,
    event: OnBatchJobFinalized,
  ) {
    val job = event.job
    if (job.type != BatchJobType.AI_REVIEW || job.hidden || job.status != BatchJobStatus.CANCELLED) return
    triggerService.abandonPendingOfCancelledRun(projectId, job.id)
  }

  private fun recordRejectionsFromManualStateChanges(
    event: OnProjectActivityEvent,
    entities: List<ActivityModifiedEntity>,
  ) {
    if (event.activityRevision.type == ActivityType.AI_REVIEW) return
    triggerService.markRejectedByStateChange(getUnreviewedTranslationIds(entities), event.activityRevision.authorId)
  }

  private fun onAiReviewJobFinalized(
    projectId: Long,
    project: ProjectDto,
    event: OnBatchJobFinalized,
  ) {
    if (aiReviewChunkProcessor.getParams(event.job).handlingStuckItems) return
    triggerService.startStuckPendingReviews(projectId, project)
  }

  private fun onQaJobFinalized(
    projectId: Long,
    project: ProjectDto,
    event: OnBatchJobFinalized,
  ) {
    val qaTargets = event.job.target.map { objectMapper.convertValue(it, BatchTranslationTargetItem::class.java) }
    triggerService.startReviewAfterQa(projectId, qaTargets)
    triggerService.startStuckPendingReviews(projectId, project)
  }

  private fun onOtherJobFinalized(
    projectId: Long,
    project: ProjectDto,
    event: OnBatchJobFinalized,
  ) {
    val activityRevisionId = event.activityRevisionId ?: return
    val entities = activityService.findModifiedEntitiesByRevisionId(activityRevisionId)
    if (entities.isEmpty()) return

    val textChanges = getTextChangedTranslations(entities).mapNotNull { it.toTextChange() }
    triggerService.startReviewForPendingKeys(projectId, project, textChanges.map { it.keyId })
    triggerService.startReevaluation(projectId, getScopeChangedKeyIds(entities))
  }

  private fun findProjectWithActiveAiReview(projectId: Long): ProjectDto? {
    val project = projectService.findDto(projectId) ?: return null
    if (!aiReviewAvailability.isVisible(project.organizationOwnerId)) return null
    if (!aiReviewRuleRepository.existsByProjectIdAndEnabledTrue(projectId)) return null
    if (!aiReviewAvailability.isAvailable(project.organizationOwnerId)) return null
    return project
  }

  private fun getTextChangedTranslations(entities: List<ActivityModifiedEntity>): List<ActivityModifiedEntity> =
    entities.filter {
      it.entityClass == Translation::class.simpleName &&
        it.revisionType != RevisionType.DEL &&
        it.modifications.containsKey("text")
    }

  private fun getUnreviewedTranslationIds(entities: List<ActivityModifiedEntity>): List<Long> =
    entities
      .filter {
        it.entityClass == Translation::class.simpleName &&
          it.revisionType != RevisionType.DEL &&
          !it.modifications.containsKey("text")
      }.filter { entity ->
        val stateModification = entity.modifications["state"] ?: return@filter false
        stateModification.new?.toString() != TranslationState.REVIEWED.name
      }.map { it.entityId }

  private fun getScopeChangedKeyIds(entities: List<ActivityModifiedEntity>): List<Long> {
    val tagChanged =
      entities
        .filter { it.entityClass == KeyMeta::class.simpleName && it.modifications.containsKey("tags") }
        .mapNotNull { it.describingRelations?.get("key")?.entityId }
    val namespaceChanged =
      entities
        .filter {
          it.entityClass == Key::class.simpleName &&
            it.revisionType != RevisionType.DEL &&
            it.modifications.containsKey("namespace")
        }.map { it.entityId }
    return (tagChanged + namespaceChanged).distinct()
  }

  private fun ActivityModifiedEntity.toTextChange(): AiReviewTriggerService.TextChange? {
    val keyId = describingRelations?.get("key")?.entityId ?: return null
    val languageId = describingRelations?.get("language")?.entityId ?: return null
    return AiReviewTriggerService.TextChange(
      translationId = entityId,
      keyId = keyId,
      languageId = languageId,
      newText = modifications["text"]?.new as String?,
    )
  }
}
