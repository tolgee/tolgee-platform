package io.tolgee.hateoas.automation

import io.tolgee.hateoas.cdn.CdnModelAssembler
import io.tolgee.model.automations.AutomationAction
import io.tolgee.model.automations.AutomationActionType
import org.springframework.stereotype.Component

@Component
class CdnExportActionFiller(
  private val cdnModelAssembler: CdnModelAssembler
) : AutomationActionModelFiller {
  override fun fill(model: AutomationActionModel, entity: AutomationAction) {
    model.cdn = entity.cdn?.let { cdnModelAssembler.toModel(it) }
      ?: throw IllegalStateException("Exporter not defined")
  }

  override val type: AutomationActionType = AutomationActionType.CDN_PUBLISH
}
