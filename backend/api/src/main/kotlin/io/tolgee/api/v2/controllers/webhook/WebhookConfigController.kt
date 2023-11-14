package io.tolgee.api.v2.controllers.webhook

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.dtos.request.WebhookConfigRequest
import io.tolgee.dtos.response.WebhookTestResponse
import io.tolgee.hateoas.webhooks.WebhookConfigModel
import io.tolgee.hateoas.webhooks.WebhookConfigModelAssembler
import io.tolgee.model.enums.Scope
import io.tolgee.model.webhook.WebhookConfig
import io.tolgee.security.ProjectHolder
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authorization.RequiresProjectPermissions
import io.tolgee.service.webhooks.WebhookConfigService
import org.springdoc.api.annotations.ParameterObject
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PagedResourcesAssembler
import org.springframework.hateoas.PagedModel
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.validation.Valid

@Suppress("MVCPathVariableInspection")
@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(
  value = [
    "/v2/projects/{projectId}/webhook-configs",
  ]
)
@Tag(name = "Webhooks", description = "Webhooks management")
class WebhookConfigController(
  private val webhookConfigService: WebhookConfigService,
  private val webhookConfigModelAssembler: WebhookConfigModelAssembler,
  @Suppress("SpringJavaInjectionPointsAutowiringInspection")
  private val pageModelAssembler: PagedResourcesAssembler<WebhookConfig>,
  private val projectHolder: ProjectHolder
) {
  @PostMapping("")
  @Operation(description = "Creates new webhook configuration")
  @RequiresProjectPermissions([Scope.WEBHOOKS_MANAGE])
  @AllowApiAccess
  fun create(@Valid @RequestBody dto: WebhookConfigRequest): WebhookConfigModel {
    val config = webhookConfigService.create(projectHolder.projectEntity, dto)
    return webhookConfigModelAssembler.toModel(config)
  }

  @PutMapping("/{id}")
  @Operation(description = "Updates webhook configuration")
  @RequiresProjectPermissions([Scope.WEBHOOKS_MANAGE])
  @AllowApiAccess
  fun update(@PathVariable id: Long, @Valid @RequestBody dto: WebhookConfigRequest): WebhookConfigModel {
    val webhookConfig = webhookConfigService.update(projectId = projectHolder.project.id, id, dto)
    return webhookConfigModelAssembler.toModel(webhookConfig)
  }

  @RequiresProjectPermissions([Scope.WEBHOOKS_MANAGE])
  @GetMapping("")
  @Operation(description = "List webhook configurations")
  @AllowApiAccess
  fun list(@ParameterObject pageable: Pageable): PagedModel<WebhookConfigModel> {
    val page = webhookConfigService.findAllInProject(projectHolder.project.id, pageable)
    return pageModelAssembler.toModel(page, webhookConfigModelAssembler)
  }

  @RequiresProjectPermissions([Scope.WEBHOOKS_MANAGE])
  @DeleteMapping("/{id}")
  @Operation(description = "Deletes webhook configuration")
  @AllowApiAccess
  fun delete(@PathVariable id: Long) {
    webhookConfigService.delete(projectHolder.project.id, id)
  }

  @RequiresProjectPermissions([Scope.WEBHOOKS_MANAGE])
  @GetMapping("/{id}")
  @Operation(description = "Get webhook configuration")
  @AllowApiAccess
  fun get(@PathVariable id: Long): WebhookConfigModel {
    return webhookConfigModelAssembler.toModel(webhookConfigService.get(projectHolder.project.id, id))
  }

  @RequiresProjectPermissions([Scope.WEBHOOKS_MANAGE])
  @PostMapping("/{id}/test")
  @Operation(description = "Tests webhook configuration")
  @AllowApiAccess
  fun test(@PathVariable id: Long): WebhookTestResponse {
    val success = webhookConfigService.test(projectHolder.project.id, id)
    return WebhookTestResponse(success)
  }
}
