package io.tolgee.component.automations

import io.tolgee.events.OnProjectActivityStoredEvent
import io.tolgee.model.Language
import io.tolgee.model.Project
import io.tolgee.model.automations.AutomationTriggerType
import io.tolgee.model.key.Key
import io.tolgee.model.translation.Translation
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

@Component
class AutomationActivityListener(
  private val automationsBatchJobCreator: AutomationsBatchJobCreator
) {
  @EventListener
  @Async
  fun listen(event: OnProjectActivityStoredEvent) {
    executeAutomations(event)
  }

  private fun executeAutomations(event: OnProjectActivityStoredEvent) {
    val projectId = event.activityRevision.projectId ?: return
    if (event.activityRevision.modifiedEntities.isEmpty()) {
      return
    }
    val activityType = event.activityRevision.type ?: return

    val translationModification = isTranslationDataModification(event)

    val triggerTypes = automationTriggerTypes(translationModification)

    automationsBatchJobCreator.executeAutomation(
      projectId,
      event.activityRevision.id,
      triggerTypes,
      activityType
    )
  }

  private fun automationTriggerTypes(translationModification: Boolean): MutableList<AutomationTriggerType> {
    val triggerTypes = mutableListOf(AutomationTriggerType.ACTIVITY)

    if (translationModification) {
      triggerTypes.add(AutomationTriggerType.TRANSLATION_DATA_MODIFICATION)
    }

    return triggerTypes
  }

  private fun isTranslationDataModification(event: OnProjectActivityStoredEvent): Boolean {
    return event.activityRevision.modifiedEntities.any { modifiedEntity ->
      arrayOf(
        Translation::class,
        Key::class,
        Language::class,
        Project::class
      ).any { allowedClass -> allowedClass.simpleName == modifiedEntity.entityClass }
    }
  }
}
