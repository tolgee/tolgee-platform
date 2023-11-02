package io.tolgee.dtos.request.automation

import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.model.automations.AutomationActionType
import javax.validation.constraints.NotNull

data class AutomationActionRequest(
  @NotNull
  var type: AutomationActionType,

  @Schema(description = """Applicable when type is CDN_PUBLISH""")
  var cdnExporterId: Long?
)
