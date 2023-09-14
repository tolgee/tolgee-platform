package io.tolgee.batch.request

import io.tolgee.dtos.cacheable.automations.AutomationActionDto
import io.tolgee.dtos.cacheable.automations.AutomationTriggerDto

data class AutomationBjRequest(
  var trigger: AutomationTriggerDto,
  var action: AutomationActionDto
)
