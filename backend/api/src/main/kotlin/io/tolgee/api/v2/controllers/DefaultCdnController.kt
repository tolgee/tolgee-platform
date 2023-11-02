package io.tolgee.api.v2.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.dtos.request.DefaultCdnDto
import io.tolgee.hateoas.automation.AutomationModel
import io.tolgee.hateoas.automation.AutomationModelAssembler
import io.tolgee.model.enums.Scope
import io.tolgee.security.ProjectHolder
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authorization.RequiresProjectPermissions
import io.tolgee.service.automations.AutomationService
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.validation.Valid

@Suppress("MVCPathVariableInspection", "SpringJavaInjectionPointsAutowiringInspection")
@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(
  value = [
    "/v2/projects/{projectId}/default-project-cdn",
  ]
)
@Tag(name = "Default project CDN", description = "Configures default project CDN in single endpoint")
class DefaultCdnController(
  private val projectHolder: ProjectHolder,
  private val automationModelAssembler: AutomationModelAssembler,
  private val automationService: AutomationService
) : IController {
  @PostMapping("")
  @Operation(description = "Create Default Project CDN Automation")
  @RequiresProjectPermissions([Scope.AUTOMATIONS_MANAGE])
  @AllowApiAccess
  fun create(@Valid @RequestBody dto: DefaultCdnDto): AutomationModel {
    val automation = automationService.createProjectDefaultCdn(projectHolder.project.id, dto)
    return automationModelAssembler.toModel(automation)
  }

  @GetMapping("")
  @Operation(description = "Get Default Project CDN Automation")
  @RequiresProjectPermissions([Scope.AUTOMATIONS_MANAGE])
  @AllowApiAccess
  fun get(): AutomationModel? {
    val automation = automationService.getDefaultProjectAutomation(projectHolder.project.id) ?: return null
    return automationModelAssembler.toModel(automation)
  }
}
