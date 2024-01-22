package io.tolgee.component.automations

import io.tolgee.events.OnProjectActivityStoredEvent
import io.tolgee.model.Language
import io.tolgee.model.Project
import io.tolgee.model.key.Key
import io.tolgee.model.translation.Translation
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

@Component
class AutomationActivityListener(
  private val automationsBatchJobCreator: AutomationsBatchJobCreator,
) {
  @EventListener
  @Async
  fun listen(event: OnProjectActivityStoredEvent) {
    executeActivityAutomationIfShould(event)
    executeTranslationDataModificationAutomationIfShould(event)
  }

  private fun executeTranslationDataModificationAutomationIfShould(event: OnProjectActivityStoredEvent) {
    val projectId = event.activityRevision.projectId ?: return
    if (isTranslationDataModification(event)) {
      automationsBatchJobCreator.executeTranslationDataModificationAutomation(projectId, event.activityRevision.id)
    }
  }

  private fun executeActivityAutomationIfShould(event: OnProjectActivityStoredEvent) {
    val activityType = event.activityRevision.type ?: return
    val projectId = event.activityRevision.projectId ?: return
    if (event.activityRevision.modifiedEntities.isEmpty()) {
      return
    }
    automationsBatchJobCreator.executeActivityAutomation(projectId, activityType, event.activityRevision.id)
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
