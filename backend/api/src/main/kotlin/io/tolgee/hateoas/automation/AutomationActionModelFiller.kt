package io.tolgee.hateoas.automation

import io.tolgee.model.automations.AutomationAction
import io.tolgee.model.automations.AutomationActionType

interface AutomationActionModelFiller {
  fun fill(
    model: AutomationActionModel,
    entity: AutomationAction,
  )

  val type: AutomationActionType
}
