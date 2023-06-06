package io.tolgee.api.v2.controllers.batch

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.dtos.request.BatchTranslateRequest
import io.tolgee.model.enums.Scope
import io.tolgee.security.AuthenticationFacade
import io.tolgee.security.apiKeyAuth.AccessWithApiKey
import io.tolgee.security.project_auth.AccessWithProjectPermission
import io.tolgee.security.project_auth.ProjectHolder
import io.tolgee.service.batch.BatchJobService
import io.tolgee.service.security.SecurityService
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.validation.Valid

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(value = ["/v2/projects/{projectId:\\d+}/batch", "/v2/projects/batch"])
@Tag(name = "Export")
@Suppress("SpringJavaInjectionPointsAutowiringInspection", "MVCPathVariableInspection")
class BatchOperationController(
  private val rabbitTemplate: RabbitTemplate,
  private val securityService: SecurityService,
  private val projectHolder: ProjectHolder,
  private val batchJobService: BatchJobService,
  private val authenticationFacade: AuthenticationFacade
) {
  @PutMapping(value = ["/translate"])
  @AccessWithApiKey()
  @AccessWithProjectPermission(Scope.TRANSLATIONS_EDIT)
  @Operation(summary = "Translates provided keys to provided languages")
  fun translate(@Valid @RequestBody data: BatchTranslateRequest) {
    securityService.checkLanguageViewPermission(projectHolder.project.id, data.targetLanguageIds)
    securityService.checkKeyIdsExistAndIsFromProject(data.keyIds, projectHolder.project.id)
    batchJobService.createTranslateJob(data, authenticationFacade.userAccountEntity)
  }
}
