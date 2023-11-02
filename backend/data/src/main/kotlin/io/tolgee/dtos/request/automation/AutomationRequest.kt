package io.tolgee.dtos.request.automation

import javax.validation.Valid
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotEmpty

class AutomationRequest(
  @NotBlank
  var name: String,

  @Valid
  @NotEmpty
  var triggers: List<AutomationTriggerRequest>,

  @Valid
  @NotEmpty
  var actions: List<AutomationActionRequest>,
)
