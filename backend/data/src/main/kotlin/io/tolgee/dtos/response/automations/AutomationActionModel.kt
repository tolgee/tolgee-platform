package io.tolgee.dtos.response.automations

import io.tolgee.dtos.request.automation.CdnPublishParamsDto
import io.tolgee.model.automations.AutomationActionType

class AutomationActionModel(
  var id: Long,
  var type: AutomationActionType,
) {
  var cdnPublishParams: CdnPublishParamsDto? = null
}
