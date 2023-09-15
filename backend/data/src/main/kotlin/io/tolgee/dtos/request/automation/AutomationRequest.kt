package io.tolgee.dtos.request.automation

import javax.validation.Valid
import javax.validation.constraints.NotEmpty

class AutomationRequest {
  @Valid
  @NotEmpty
  var triggers: List<AutomationTriggerRequest> = listOf()
  @Valid
  @NotEmpty
  var actions: List<AutomationActionRequest> = listOf()
}
