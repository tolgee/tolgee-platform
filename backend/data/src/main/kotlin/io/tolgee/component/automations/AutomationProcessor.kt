package io.tolgee.component.automations

import io.tolgee.dtos.request.automation.AutomationActionRequest
import io.tolgee.model.automations.AutomationAction

interface AutomationProcessor {
  fun process(action: AutomationAction)
  fun fillEntity(request: AutomationActionRequest, entity: AutomationAction)
}
