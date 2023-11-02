package io.tolgee.hateoas.automation

import io.tolgee.hateoas.cdn.CdnExporterModelAssembler
import io.tolgee.model.automations.AutomationAction
import io.tolgee.model.automations.AutomationActionType
import org.springframework.stereotype.Component

@Component
class CdnExportActionFiller(
  private val cdnExporterModelAssembler: CdnExporterModelAssembler
) : AutomationActionModelFiller {
  override fun fill(model: AutomationActionModel, entity: AutomationAction) {
    model.cdnExporter = entity.cdnExporter?.let { cdnExporterModelAssembler.toModel(it) }
      ?: throw IllegalStateException("Exporter not defined")
  }

  override val type: AutomationActionType = AutomationActionType.CDN_PUBLISH
}
