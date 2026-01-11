package io.tolgee.component.automations

import io.tolgee.model.automations.AutomationAction

interface AutomationProcessor {
  fun process(
    action: AutomationAction,
    activityRevisionId: Long?,
  )
}
