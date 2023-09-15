package io.tolgee.dtos.request.automation

import io.tolgee.model.automations.AutomationActionType
import javax.validation.Valid
import javax.validation.constraints.NotNull

data class AutomationActionRequest(
  @NotNull
  var type: AutomationActionType,

  @Valid
  var cdnPublishParams: CdnPublishParamsDto? = null
)
