package io.tolgee.component.automations

import io.tolgee.activity.ActivityService
import io.tolgee.batch.events.OnBatchJobFinalized
import io.tolgee.events.OnProjectActivityStoredEvent
import io.tolgee.model.Language
import io.tolgee.model.Project
import io.tolgee.model.key.Key
import io.tolgee.model.translation.Translation
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionalEventListener

@Component
class AutomationActivityListener(
  private val automationsBatchJobCreator: AutomationsBatchJobCreator,
  private val activityService: ActivityService,
) {
  @EventListener
  @Async
  fun listen(event: OnProjectActivityStoredEvent) {
    val activityType = event.activityRevision.type ?: return
    val projectId = event.activityRevision.projectId ?: return
    if (event.activityRevision.modifiedEntities.isEmpty()) {
      return
    }

    // don't run automations on batch jobs that haven't been merged yet
    // this would lead to spamming
    if (event.activityRevision.batchJobChunkExecution != null) {
      return
    }

    automationsBatchJobCreator.executeActivityAutomation(projectId, activityType, event.activityRevision.id)

    if (isTranslationDataModification(event)) {
      automationsBatchJobCreator.executeTranslationDataModificationAutomation(projectId, event.activityRevision.id)
    }
  }

  @TransactionalEventListener
  @Async
  fun listen(event: OnBatchJobFinalized) {
    val revision = activityService.findActivityRevisionInfo(event.activityRevisionId) ?: return
    if (revision.modifiedEntityCount == 0) {
      return
    }

    val projectId = revision.projectId ?: return

    automationsBatchJobCreator.executeActivityAutomation(projectId, revision.type, revision.id)

    if (revision.isTranslationModification) {
      automationsBatchJobCreator.executeTranslationDataModificationAutomation(projectId, revision.id)
    }
  }

  private fun isTranslationDataModification(event: OnProjectActivityStoredEvent): Boolean {
    return event.activityRevision.modifiedEntities.any { modifiedEntity ->
      arrayOf(
        Translation::class,
        Key::class,
        Language::class,
        Project::class,
      ).any { allowedClass -> allowedClass.simpleName == modifiedEntity.entityClass }
    }
  }
}
