package io.tolgee.component.automations.processors

import io.tolgee.component.automations.AutomationProcessor
import io.tolgee.component.cdn.CdnUploader
import io.tolgee.constants.Message
import io.tolgee.dtos.request.automation.AutomationActionRequest
import io.tolgee.exceptions.BadRequestException
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

  override fun fillEntity(request: AutomationActionRequest, entity: AutomationAction) {
    val exporterId = request.cdnExporterId ?: throw BadRequestException(Message.CDN_EXPORTER_ID_NOT_PROVIDED)
    entity.cdnExporter = cdnExporterService.get(projectHolder.project.id, exporterId)
  }
}
