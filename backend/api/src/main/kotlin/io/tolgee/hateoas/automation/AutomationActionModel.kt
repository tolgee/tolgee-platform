package io.tolgee.hateoas.automation

import io.tolgee.hateoas.cdn.CdnExporterModel
import io.tolgee.model.automations.AutomationActionType

class AutomationActionModel(
  var id: Long,
  var type: AutomationActionType,
) {
  var cdnExporter: CdnExporterModel? = null
}
