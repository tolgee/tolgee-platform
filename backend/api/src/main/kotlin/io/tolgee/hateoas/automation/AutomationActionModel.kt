package io.tolgee.hateoas.automation

import io.tolgee.hateoas.cdn.CdnModel
import io.tolgee.model.automations.AutomationActionType

class AutomationActionModel(
  var id: Long,
  var type: AutomationActionType,
) {
  var cdn: CdnModel? = null
}
