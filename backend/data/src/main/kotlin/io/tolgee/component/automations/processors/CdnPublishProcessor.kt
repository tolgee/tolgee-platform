package io.tolgee.component.automations.processors

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.convertValue
import io.tolgee.component.automations.AutomationProcessor
import io.tolgee.component.cdn.CdnUploader
import io.tolgee.constants.Message
import io.tolgee.dtos.request.automation.AutomationActionRequest
import io.tolgee.dtos.request.automation.CdnPublishParamsDto
import io.tolgee.dtos.response.automations.AutomationActionModel
import io.tolgee.exceptions.BadRequestException
import io.tolgee.model.automations.AutomationAction
import io.tolgee.model.automations.params.CdnPublishParams
import io.tolgee.model.enums.Scope
import io.tolgee.security.project_auth.ProjectHolder
import io.tolgee.service.CdnService
import io.tolgee.service.security.SecurityService
import org.springframework.stereotype.Component

@Component
class CdnPublishProcessor(
  val cdnUploader: CdnUploader,
  val cdnService: CdnService,
  val securityService: SecurityService,
  val projectHolder: ProjectHolder,
  val objectMapper: ObjectMapper
) : AutomationProcessor {
  override fun process(action: AutomationAction) {
    val params = parseParams(action)
    val cdn = cdnService.get(params.cdnId)
    cdnUploader.upload(cdnId = cdn.id, cdn.exportParams)
  }

  override fun getParamsFromRequest(request: AutomationActionRequest): Any? {
    val params = request.cdnPublishParams?.let { CdnPublishParams(it.cdnId) }
      ?: throw BadRequestException(Message.CDN_PUBLISH_PARAMS_PARAMS_NOT_PROVIDED)
    validateRequestParams(params)
    return params
  }

  override fun fillModel(model: AutomationActionModel, action: AutomationAction) {
    val params = parseParams(action)
    model.cdnPublishParams = CdnPublishParamsDto(params.cdnId)
  }

  fun validateRequestParams(params: CdnPublishParams) {
    cdnService.get(projectHolder.project.id, params.cdnId)
    securityService.checkProjectPermission(projectHolder.project.id, Scope.CDN_MANAGE)
  }

  fun parseParams(action: AutomationAction): CdnPublishParams {
    return action.params?.let { objectMapper.convertValue(it) }
      ?: throw IllegalStateException("Wrong params passed to cdn publish processor")
  }
}
