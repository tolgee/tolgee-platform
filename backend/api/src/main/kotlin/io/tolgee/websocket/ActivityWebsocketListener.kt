package io.tolgee.websocket

import io.tolgee.activity.projectActivity.RelationDescriptionExtractor
import io.tolgee.batch.OnBatchJobCompleted
import io.tolgee.batch.WebsocketProgressInfo
import io.tolgee.batch.events.OnBatchJobCancelled
import io.tolgee.batch.events.OnBatchJobFailed
import io.tolgee.batch.events.OnBatchJobProgress
import io.tolgee.batch.events.OnBatchJobSucceeded
import io.tolgee.component.CurrentDateProvider
import io.tolgee.constants.Message
import io.tolgee.events.OnProjectActivityStoredEvent
import io.tolgee.hateoas.userAccount.SimpleUserAccountModelAssembler
import io.tolgee.model.activity.ActivityModifiedEntity
import io.tolgee.model.activity.ActivityRevision
import io.tolgee.model.batch.BatchJobStatus
import io.tolgee.model.key.Key
import io.tolgee.model.translation.Translation
import io.tolgee.service.security.UserAccountService
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class ActivityWebsocketListener(
  private val websocketEventPublisher: WebsocketEventPublisher,
  private val simpleUserAccountModelAssembler: SimpleUserAccountModelAssembler,
  private val userAccountService: UserAccountService,
  private val relationDescriptionExtractor: RelationDescriptionExtractor,
  private val currentDateProvider: CurrentDateProvider,
) {
  @Async
  @EventListener
  fun onActivity(event: OnProjectActivityStoredEvent) {
    event.activityRevision.projectId ?: return

    val translationDataModifications =
      event.activityRevision.modifiedEntities.filter {
        it.entityClass == Key::class.simpleName || it.entityClass == Translation::class.simpleName
      }

    if (translationDataModifications.isNotEmpty()) {
      onTranslationDataModified(
        event.activityRevision,
        translationDataModifications,
      )
    }
  }

  fun getActorInfo(userId: Long?): ActorInfo {
    return userId?.let {
      val user = userAccountService.findDto(userId) ?: return@let null
      ActorInfo(
        type = ActorType.USER,
        data = simpleUserAccountModelAssembler.toModel(user),
      )
    } ?: ActorInfo(type = ActorType.UNKNOWN, data = null)
  }

  private fun onTranslationDataModified(
    activityRevision: ActivityRevision,
    translationDataModifications: List<ActivityModifiedEntity>,
  ) {
    val data =
      if (translationDataModifications.size < 500) {
        mapOf(
          "keys" to
            translationDataModifications.filter { it.entityClass == Key::class.simpleName }.map {
              getModifiedEntityView(it)
            },
          "translations" to
            translationDataModifications.filter { it.entityClass == Translation::class.simpleName }.map {
              getModifiedEntityView(it)
            },
        )
      } else {
        null
      }

    websocketEventPublisher(
      "/projects/${activityRevision.projectId!!}/${WebsocketEventType.TRANSLATION_DATA_MODIFIED.typeName}",
      WebsocketEvent(
        actor = getActorInfo(activityRevision.authorId),
        data = data,
        sourceActivity = activityRevision.type,
        activityId = activityRevision.id,
        dataCollapsed = data == null,
        timestamp = currentDateProvider.date.time,
      ),
    )
  }

  @EventListener(OnBatchJobProgress::class)
  fun onBatchJobProgress(event: OnBatchJobProgress) {
    if (event.job.hidden) return

    val realStatus =
      if (event.job.status == BatchJobStatus.PENDING) {
        BatchJobStatus.RUNNING
      } else {
        event.job.status
      }

    websocketEventPublisher(
      "/projects/${event.job.projectId}/${WebsocketEventType.BATCH_JOB_PROGRESS.typeName}",
      WebsocketEvent(
        actor = getActorInfo(event.job.authorId),
        data = WebsocketProgressInfo(event.job.id, event.processed, event.total, realStatus),
        sourceActivity = null,
        activityId = null,
        dataCollapsed = false,
        timestamp = currentDateProvider.date.time,
      ),
    )
  }

  @TransactionalEventListener(OnBatchJobSucceeded::class, phase = TransactionPhase.AFTER_COMMIT)
  fun onBatchJobSucceeded(event: OnBatchJobSucceeded) {
    onBatchJobCompleted(event)
  }

  @TransactionalEventListener(OnBatchJobFailed::class)
  fun onBatchJobFailed(event: OnBatchJobFailed) {
    onBatchJobCompleted(event, event.errorMessage)
  }

  @TransactionalEventListener(OnBatchJobCancelled::class)
  fun onBatchJobCancelled(event: OnBatchJobCancelled) {
    onBatchJobCompleted(event)
  }

  fun onBatchJobCompleted(
    event: OnBatchJobCompleted,
    errorMessage: Message? = null,
  ) {
    if (event.job.hidden && event.job.status != BatchJobStatus.FAILED) return

    websocketEventPublisher(
      "/projects/${event.job.projectId}/${WebsocketEventType.BATCH_JOB_PROGRESS.typeName}",
      WebsocketEvent(
        actor = getActorInfo(event.job.authorId),
        data = WebsocketProgressInfo(event.job.id, null, null, event.job.status, errorMessage?.code),
        sourceActivity = null,
        activityId = null,
        dataCollapsed = false,
        timestamp = currentDateProvider.date.time,
      ),
    )
  }

  private fun getModifiedEntityView(it: ActivityModifiedEntity): MutableMap<String, Any?> {
    val data = mutableMapOf<String, Any?>("id" to it.entityId)
    it.describingData?.let { describingData -> data.putAll(describingData) }
    data["modifications"] = it.modifications
    data["changeType"] = it.revisionType
    data["relations"] =
      it.describingRelations
        ?.map { relationsEntry ->
          relationsEntry.key to
            relationDescriptionExtractor.extract(
              relationsEntry.value,
              it.activityRevision.describingRelations,
            )
        }?.toMap()
    return data
  }
}
