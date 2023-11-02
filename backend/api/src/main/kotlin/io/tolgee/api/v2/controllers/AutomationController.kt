package io.tolgee.api.v2.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.dtos.request.automation.AutomationRequest
import io.tolgee.hateoas.automation.AutomationModel
import io.tolgee.hateoas.automation.AutomationModelAssembler
import io.tolgee.model.automations.Automation
import io.tolgee.model.enums.Scope
import io.tolgee.security.ProjectHolder
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authorization.RequiresProjectPermissions
import io.tolgee.service.automations.AutomationService
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

@Suppress("MVCPathVariableInspection", "SpringJavaInjectionPointsAutowiringInspection")
@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(
  value = [
    "/v2/projects/{projectId}/automations",
  ]
)
@Tag(name = "Automation management", description = "Endpoints for Automation management")
class AutomationController(
  private val projectHolder: ProjectHolder,
  private val automationModelAssembler: AutomationModelAssembler,
  private val pagedCdnModelAssembler: PagedResourcesAssembler<Automation>,
  private val automationService: AutomationService
) : IController {
  @PostMapping("")
  @Operation(description = "Create Automation")
  @RequiresProjectPermissions([Scope.AUTOMATIONS_MANAGE])
  @AllowApiAccess
  fun create(@Valid @RequestBody dto: AutomationRequest): AutomationModel {
    val automation = automationService.create(projectHolder.project.id, dto)
    return automationModelAssembler.toModel(automation)
  }

  @PutMapping("/{automationId}")
  @Operation(description = "Create Automation")
  @RequiresProjectPermissions([Scope.AUTOMATIONS_MANAGE])
  @AllowApiAccess
  fun update(@PathVariable automationId: Long, @Valid @RequestBody dto: AutomationRequest): AutomationModel {
    val automation = automationService.update(projectHolder.project.id, automationId, dto)
    return automationModelAssembler.toModel(automation)
  }

  @RequiresProjectPermissions([Scope.AUTOMATIONS_MANAGE])
  @GetMapping("")
  @Operation(description = "List existing Automations")
  @AllowApiAccess
  fun list(@ParameterObject pageable: Pageable): PagedModel<AutomationModel> {
    val page = automationService.getProjectAutomations(projectHolder.project.id, pageable)
    return pagedCdnModelAssembler.toModel(page, automationModelAssembler)
  }

  @RequiresProjectPermissions([Scope.AUTOMATIONS_MANAGE])
  @DeleteMapping("/{automationId}")
  @Operation(description = "Delete Automation")
  @AllowApiAccess
  fun delete(@PathVariable automationId: Long) {
    automationService.delete(projectHolder.project.id, automationId)
  }

  @RequiresProjectPermissions([Scope.AUTOMATIONS_MANAGE])
  @GetMapping("/{automationId}")
  @Operation(description = "Get Automation")
  @AllowApiAccess
  fun get(@PathVariable automationId: Long): AutomationModel {
    return automationModelAssembler.toModel(automationService.get(projectHolder.project.id, automationId))
  }
}