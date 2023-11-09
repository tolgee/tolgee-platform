package io.tolgee.component.automations.processors

import io.tolgee.component.automations.AutomationProcessor
import io.tolgee.component.cdn.CdnUploader
import io.tolgee.model.automations.AutomationAction
import io.tolgee.security.ProjectHolder
import io.tolgee.service.cdn.CdnExporterService
import io.tolgee.service.security.SecurityService
import org.springframework.stereotype.Component

@Component
class CdnPublishProcessor(
  val cdnUploader: CdnUploader,
  val cdnExporterService: CdnExporterService,
  val securityService: SecurityService,
  val projectHolder: ProjectHolder,
) : AutomationProcessor {
  override fun process(action: AutomationAction) {
    val exporter = action.cdnExporter ?: throw IllegalStateException("Wrong params passed to cdn publish processor")
    cdnUploader.upload(cdnExporterId = exporter.id)
  }
}
