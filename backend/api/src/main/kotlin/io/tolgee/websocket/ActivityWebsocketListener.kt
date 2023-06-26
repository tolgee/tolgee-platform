package io.tolgee.websocket

import io.tolgee.activity.projectActivityView.RelationDescriptionExtractor
import io.tolgee.batch.OnBatchOperationCompleted
import io.tolgee.batch.WebsocketProgressInfo
import io.tolgee.batch.events.OnBatchOperationCancelled
import io.tolgee.batch.events.OnBatchOperationFailed
import io.tolgee.batch.events.OnBatchOperationProgress
import io.tolgee.batch.events.OnBatchOperationSucceeded
import io.tolgee.events.OnProjectActivityStoredEvent
import io.tolgee.hateoas.user_account.SimpleUserAccountModelAssembler
import io.tolgee.model.activity.ActivityModifiedEntity
import io.tolgee.model.activity.ActivityRevision
import io.tolgee.model.batch.BatchJobStatus
import io.tolgee.model.key.Key
import io.tolgee.model.translation.Translation
import io.tolgee.service.security.UserAccountService
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionalEventListener

@Component
class ActivityWebsocketListener(
  private val websocketEventPublisher: WebsocketEventPublisher,
  private val simpleUserAccountModelAssembler: SimpleUserAccountModelAssembler,
  private val userAccountService: UserAccountService,
  private val relationDescriptionExtractor: RelationDescriptionExtractor
) {

  @Async
  @EventListener
  fun onActivity(event: OnProjectActivityStoredEvent) {
    event.activityRevision.projectId ?: return

    val translationDataModifications = event.activityRevision.modifiedEntities.filter {
      it.entityClass == Key::class.simpleName || it.entityClass == Translation::class.simpleName
    }

    if (translationDataModifications.isNotEmpty()) {
      onTranslationDataModified(
        event.activityRevision,
        translationDataModifications
      )
    }
  }

  fun getActorInfo(userId: Long?): ActorInfo {
    return userId?.let {
      val user = userAccountService.findDto(userId) ?: return@let null
      ActorInfo(
        type = ActorType.USER,
        data = simpleUserAccountModelAssembler.toModel(user)
      )
    } ?: ActorInfo(type = ActorType.UNKNOWN, data = null)
  }

  private fun onTranslationDataModified(
    activityRevision: ActivityRevision,
    translationDataModifications: List<ActivityModifiedEntity>,
  ) {
    val data = if (translationDataModifications.size < 500) {
      mapOf(
        "keys" to translationDataModifications.filter { it.entityClass == Key::class.simpleName }.map {
          getModifiedEntityView(it)
        },
        "translations" to translationDataModifications.filter { it.entityClass == Translation::class.simpleName }.map {
          getModifiedEntityView(it)
        }
      )
    } else null

    websocketEventPublisher(
      "/projects/${activityRevision.projectId!!}/${WebsocketEventType.TRANSLATION_DATA_MODIFIED.typeName}",
      WebsocketEvent(
        actor = getActorInfo(activityRevision.authorId),
        data = data,
        sourceActivity = activityRevision.type,
        activityId = activityRevision.id,
        dataCollapsed = data == null
      )
    )
  }

  @EventListener(OnBatchOperationProgress::class)
  fun onBatchOperationProgress(event: OnBatchOperationProgress) {
    websocketEventPublisher(
      "/projects/${event.job.projectId}/${WebsocketEventType.BATCH_OPERATION_PROGRESS.typeName}",
      WebsocketEvent(
        actor = getActorInfo(event.job.authorId),
        data = WebsocketProgressInfo(event.job.id, event.processed, event.total, BatchJobStatus.RUNNING),
        sourceActivity = null,
        activityId = null,
        dataCollapsed = false
      )
    )
  }

  @TransactionalEventListener(OnBatchOperationSucceeded::class)
  fun onBatchOperationSucceeded(event: OnBatchOperationSucceeded) {
    onBatchOperationCompleted(event)
  }

  @TransactionalEventListener(OnBatchOperationFailed::class)
  fun onBatchOperationFailed(event: OnBatchOperationFailed) {
    onBatchOperationCompleted(event)
  }

  @TransactionalEventListener(OnBatchOperationCancelled::class)
  fun onBatchOperationCancelled(event: OnBatchOperationCancelled) {
    onBatchOperationCompleted(event)
  }

  fun onBatchOperationCompleted(event: OnBatchOperationCompleted) {
    websocketEventPublisher(
      "/projects/${event.job.projectId}/${WebsocketEventType.BATCH_OPERATION_PROGRESS.typeName}",
      WebsocketEvent(
        actor = getActorInfo(event.job.authorId),
        data = WebsocketProgressInfo(event.job.id, null, null, event.job.status),
        sourceActivity = null,
        activityId = null,
        dataCollapsed = false
      )
    )
  }

  private fun getModifiedEntityView(it: ActivityModifiedEntity): MutableMap<String, Any?> {
    val data = mutableMapOf<String, Any?>("id" to it.entityId)
    it.describingData?.let { describingData -> data.putAll(describingData) }
    data["modifications"] = it.modifications
    data["changeType"] = it.revisionType
    data["relations"] = it.describingRelations?.map { relationsEntry ->
      relationsEntry.key to relationDescriptionExtractor.extract(
        relationsEntry.value,
        it.activityRevision.describingRelations
      )
    }?.toMap()
    return data
  }
}
