package io.tolgee.component.automations

import io.tolgee.dtos.request.automation.AutomationActionRequest
import io.tolgee.dtos.response.automations.AutomationActionModel
import io.tolgee.model.automations.AutomationAction

interface AutomationProcessor {
  fun process(action: AutomationAction)

  fun getParamsFromRequest(request: AutomationActionRequest): Any?
  fun fillModel(model: AutomationActionModel, action: AutomationAction)
}
