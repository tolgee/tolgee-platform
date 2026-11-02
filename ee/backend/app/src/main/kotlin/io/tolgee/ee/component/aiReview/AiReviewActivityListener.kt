package io.tolgee.ee.component.aiReview

import io.tolgee.activity.ActivityService
import io.tolgee.activity.data.EntityDescription
import io.tolgee.activity.data.EntityDescriptionRef
import io.tolgee.activity.data.RevisionType
import io.tolgee.batch.data.BatchJobDto
import io.tolgee.batch.data.BatchJobType
import io.tolgee.batch.data.BatchTranslationTargetItem
import io.tolgee.batch.events.OnBatchJobFinalized
import io.tolgee.component.eventListeners.BypassableActivityListener
import io.tolgee.ee.service.aiReview.AiReviewQueueService
import io.tolgee.events.OnProjectActivityEvent
import io.tolgee.events.OnQaIssuesIgnored
import io.tolgee.model.Project
import io.tolgee.model.activity.ActivityModifiedEntity
import io.tolgee.model.batch.params.AiReviewJobParams
import io.tolgee.model.batch.params.QaCheckJobParams
import io.tolgee.model.translation.Translation
import io.tolgee.repository.TranslationRepository
import io.tolgee.service.language.LanguageService
import io.tolgee.util.runSentryCatching
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

/**
 * Follows the pattern of QaActivityListener: normal requests arrive through
 * [OnProjectActivityEvent] (batch chunks are skipped there), batch jobs through
 * [OnBatchJobFinalized] with the merged revision.
 */
@Component
class AiReviewActivityListener(
  private val activityService: ActivityService,
  private val aiReviewQueueService: AiReviewQueueService,
  private val languageService: LanguageService,
  private val translationRepository: TranslationRepository,
  private val objectMapper: ObjectMapper,
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
      process(projectId, event.modifiedEntities.values.flatMap { it.values })
    }
  }

  @EventListener
  fun onBatchJobFinalized(event: OnBatchJobFinalized) {
    if (bypass) return
    runSentryCatching {
      val projectId = event.job.projectId ?: return
      if (event.job.type == BatchJobType.AI_REVIEW) return
      if (event.job.type == BatchJobType.QA_CHECK) {
        handleQaFinalized(event, projectId)
        return
      }

      val activityRevisionId = event.activityRevisionId ?: return
      val entities = activityService.findModifiedEntitiesByRevisionId(activityRevisionId)
      process(projectId, entities)
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
  fun onSelfHeal(event: OnBatchJobFinalized) {
    if (bypass) return
    if (event.job.type != BatchJobType.QA_CHECK && event.job.type != BatchJobType.AI_REVIEW) return
    val projectId = event.job.projectId ?: return
    if (isSelfHealJob(event.job)) return
    runSentryCatching {
      aiReviewQueueService.selfHeal(projectId, event.job.id)
    }
  }

  private fun isSelfHealJob(job: BatchJobDto): Boolean {
    val params = job.params ?: return false
    if (job.type == BatchJobType.AI_REVIEW) {
      return objectMapper.convertValue(params, AiReviewJobParams::class.java).handlingStuckItems
    }
    return objectMapper.convertValue(params, QaCheckJobParams::class.java).handlingStuckStaleItems
  }

  private fun process(
    projectId: Long,
    entities: List<ActivityModifiedEntity>,
  ) {
    val textChanged = textChangedTranslations(entities)
    if (textChanged.isEmpty()) return

    val baseLanguageId = languageService.getProjectBaseLanguage(projectId).id
    val baseKeyIds =
      textChanged
        .filter { it.describingRelations?.get("language")?.entityId == baseLanguageId }
        .mapNotNull { it.describingRelations?.get("key")?.entityId }
    aiReviewQueueService.onSourceChanged(projectId, baseKeyIds)
    aiReviewQueueService.onTextChanged(projectId, textChanged.map { it.entityId })
  }

  private fun handleQaFinalized(
    event: OnBatchJobFinalized,
    projectId: Long,
  ) {
    val translationIds =
      event.job.target.mapNotNull { target ->
        val item = objectMapper.convertValue(target, BatchTranslationTargetItem::class.java)
        translationRepository
          .findOneByProjectIdAndKeyIdAndLanguageId(projectId, item.keyId, item.languageId)
          ?.id
      }
    aiReviewQueueService.startJobsForReady(projectId, translationIds)
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
    val newBaseLanguageId =
      projectEntities.values
        .filter { it.revisionType == RevisionType.MOD }
        .firstNotNullOfOrNull { entityIdOf(it.modifications["baseLanguage"]?.new) }
        ?: return
    aiReviewQueueService.onBaseLanguageChanged(projectId, newBaseLanguageId)
  }

  private fun entityIdOf(value: Any?): Long? {
    if (value is EntityDescription) return value.entityId
    if (value is EntityDescriptionRef) return value.entityId
    if (value is Map<*, *>) return (value["entityId"] as? Number)?.toLong()
    return null
  }

  private fun textChangedTranslations(entities: List<ActivityModifiedEntity>): List<ActivityModifiedEntity> =
    entities.filter {
      it.entityClass == Translation::class.simpleName &&
        it.revisionType != RevisionType.DEL &&
        it.modifications.containsKey("text")
    }
}
