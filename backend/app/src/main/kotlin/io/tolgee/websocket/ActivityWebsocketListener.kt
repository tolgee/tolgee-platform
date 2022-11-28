package io.tolgee.websocket

import io.tolgee.activity.projectActivityView.RelationDescriptionExtractor
import io.tolgee.api.v2.hateoas.user_account.SimpleUserAccountModelAssembler
import io.tolgee.events.OnProjectActivityStoredEvent
import io.tolgee.model.activity.ActivityModifiedEntity
import io.tolgee.model.activity.ActivityRevision
import io.tolgee.model.key.Key
import io.tolgee.model.translation.Translation
import io.tolgee.service.security.UserAccountService
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

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
      val user = userAccountService.findActive(userId) ?: return@let null
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
      "/projects/${activityRevision.projectId!!}/${Types.TRANSLATION_DATA_MODIFIED.typeName}",
      WebsocketEvent(
        actor = getActorInfo(activityRevision.authorId),
        data = data,
        sourceActivity = activityRevision.type,
        activityId = activityRevision.id,
        dataCollapsed = data == null
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
