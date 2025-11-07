package io.tolgee.activity

import io.tolgee.activity.data.ActivityType
import io.tolgee.events.OnProjectActivityStoredEvent
import io.tolgee.model.activity.ActivityRevision
import io.tolgee.model.key.Key
import io.tolgee.model.translation.Translation
import io.tolgee.service.activityNotification.ActivityNotificationService
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

@Component
class ActivityNotificationListener(
  private val activityNotificationService: ActivityNotificationService,
) {
  private val handledActivityEntities =
    mapOf(
      ActivityType.CREATE_KEY to requireNotNull(Key::class.simpleName),
      ActivityType.SET_TRANSLATIONS to requireNotNull(Translation::class.simpleName),
    )

  @EventListener
  @Async
  fun onProjectActivityStored(event: OnProjectActivityStoredEvent) {
    if (event.activityRevision.modifiedEntities.isEmpty()) return
    if (event.activityRevision.batchJobChunkExecution != null) return

    if (event.activityRevision.type !in handledActivityEntities.keys) return

    handleActivityRevision(
      event.activityRevision,
    )
  }

  private fun handleActivityRevision(activityRevision: ActivityRevision) {
    val activityType = activityRevision.type!!
    val expectedEntityClass = handledActivityEntities[activityType] ?: return
    activityRevision.modifiedEntities
      .asSequence()
      .filter { it.entityClass == expectedEntityClass }
      .forEach { modifiedEntity ->
        activityNotificationService.createBatchJob(activityRevision, modifiedEntity)
      }
  }
}
