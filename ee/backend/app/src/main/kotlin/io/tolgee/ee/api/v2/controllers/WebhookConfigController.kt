package io.tolgee.ee.api.v2.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.activity.RequestActivity
import io.tolgee.activity.data.ActivityType
import io.tolgee.component.enabledFeaturesProvider.EnabledFeaturesProvider
import io.tolgee.constants.Feature
import io.tolgee.dtos.request.WebhookConfigRequest
import io.tolgee.ee.api.v2.hateoas.assemblers.WebhookConfigModelAssembler
import io.tolgee.ee.data.WebhookTestResponse
import io.tolgee.ee.service.WebhookConfigService
import io.tolgee.model.enums.Scope
import io.tolgee.model.webhook.WebhookConfig
import io.tolgee.openApiDocs.OpenApiEeExtension
import io.tolgee.security.ProjectHolder
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authorization.RequiresProjectPermissions
import jakarta.validation.Valid
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PagedResourcesAssembler
import org.springframework.hateoas.PagedModel
import org.springframework.web.bind.annotation.*

@Suppress("MVCPathVariableInspection")
@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(
  value = [
    "/v2/projects/{projectId:[0-9]+}/webhook-configs",
  ],
)
@Tag(name = "Webhooks configuration")
@OpenApiEeExtension
class WebhookConfigController(
  private val webhookConfigService: WebhookConfigService,
  private val webhookConfigModelAssembler: WebhookConfigModelAssembler,
  @Suppress("SpringJavaInjectionPointsAutowiringInspection")
  private val pageModelAssembler: PagedResourcesAssembler<WebhookConfig>,
  private val projectHolder: ProjectHolder,
  private val enabledFeaturesProvider: EnabledFeaturesProvider,
) {
  @PostMapping("")
  @Operation(summary = "Create new webhook configuration")
  @RequiresProjectPermissions([Scope.WEBHOOKS_MANAGE])
  @AllowApiAccess
  @RequestActivity(ActivityType.WEBHOOK_CONFIG_CREATE)
  fun create(
    @Valid @RequestBody
    dto: WebhookConfigRequest,
  ): io.tolgee.hateoas.ee.webhooks.WebhookConfigModel {
    enabledFeaturesProvider.checkFeatureEnabled(
      organizationId = projectHolder.project.organizationOwnerId,
      Feature.WEBHOOKS,
    )
    val config = webhookConfigService.create(projectHolder.projectEntity, dto)
    return webhookConfigModelAssembler.toModel(config)
  }

  @PutMapping("/{id}")
  @Operation(summary = "Update webhook configuration")
  @RequiresProjectPermissions([Scope.WEBHOOKS_MANAGE])
  @AllowApiAccess
  @RequestActivity(ActivityType.WEBHOOK_CONFIG_UPDATE)
  fun update(
    @PathVariable
    id: Long,
    @Valid @RequestBody
    dto: WebhookConfigRequest,
  ): io.tolgee.hateoas.ee.webhooks.WebhookConfigModel {
    enabledFeaturesProvider.checkFeatureEnabled(
      organizationId = projectHolder.project.organizationOwnerId,
      Feature.WEBHOOKS,
    )
    val webhookConfig = webhookConfigService.update(projectId = projectHolder.project.id, id, dto)
    return webhookConfigModelAssembler.toModel(webhookConfig)
  }

  @RequiresProjectPermissions([Scope.WEBHOOKS_MANAGE])
  @GetMapping("")
  @Operation(summary = "List webhook configurations")
  @AllowApiAccess
  fun list(
    @ParameterObject pageable: Pageable,
  ): PagedModel<io.tolgee.hateoas.ee.webhooks.WebhookConfigModel> {
    val page = webhookConfigService.findAllInProject(projectHolder.project.id, pageable)
    return pageModelAssembler.toModel(page, webhookConfigModelAssembler)
  }

  @RequiresProjectPermissions([Scope.WEBHOOKS_MANAGE])
  @DeleteMapping("/{id}")
  @Operation(summary = "Delete webhook configuration")
  @AllowApiAccess
  @RequestActivity(ActivityType.WEBHOOK_CONFIG_DELETE)
  fun delete(
    @PathVariable id: Long,
  ) {
    webhookConfigService.delete(projectHolder.project.id, id)
  }

  @RequiresProjectPermissions([Scope.WEBHOOKS_MANAGE])
  @GetMapping("/{id}")
  @Operation(summary = "Get one webhook configuration")
  @AllowApiAccess
  fun get(
    @PathVariable id: Long,
  ): io.tolgee.hateoas.ee.webhooks.WebhookConfigModel {
    return webhookConfigModelAssembler.toModel(webhookConfigService.get(projectHolder.project.id, id))
  }

  @RequiresProjectPermissions([Scope.WEBHOOKS_MANAGE])
  @PostMapping("/{id}/test")
  @Operation(summary = "Test webhook configuration", description = "Sends a test request to the webhook")
  @AllowApiAccess
  fun test(
    @PathVariable id: Long,
  ): WebhookTestResponse {
    enabledFeaturesProvider.checkFeatureEnabled(
      organizationId = projectHolder.project.organizationOwnerId,
      Feature.WEBHOOKS,
    )
    val success = webhookConfigService.test(projectHolder.project.id, id)
    return WebhookTestResponse(success)
  }
}
