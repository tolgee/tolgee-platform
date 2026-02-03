package io.tolgee.dtos.cacheable.automations

import io.tolgee.model.automations.Automation

data class AutomationDto(
  val triggers: List<AutomationTriggerDto>,
  val actions: List<AutomationActionDto>,
) {
  companion object {
    fun fromEntity(entity: Automation): AutomationDto {
      return AutomationDto(
        triggers =
          entity.triggers.map {
            AutomationTriggerDto(
              it.id,
              it.type,
              it.activityType,
              it.debounceDurationInMs,
            )
          },
        actions = entity.actions.map { AutomationActionDto(it.id, it.type) },
      )
    }
  }
}
