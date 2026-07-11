/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.api.v2.controllers.project

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.hateoas.project.ProjectTransferOptionModel
import io.tolgee.model.enums.Scope
import io.tolgee.security.ProjectHolder
import io.tolgee.security.authentication.RequiresSuperAuthentication
import io.tolgee.security.authorization.RequiresProjectPermissions
import io.tolgee.service.organization.OrganizationRoleService
import io.tolgee.service.organization.OrganizationService
import io.tolgee.service.project.ProjectService
import org.springframework.data.domain.PageRequest
import org.springframework.hateoas.CollectionModel
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Suppress(names = ["MVCPathVariableInspection", "SpringJavaInjectionPointsAutowiringInspection"])
@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(value = ["/v2/projects"])
@Tag(name = "Project Transferring", description = "These endpoints manage transferring projects to other organizations")
class ProjectsTransferringController(
  private val projectService: ProjectService,
  private val projectHolder: ProjectHolder,
  private val organizationService: OrganizationService,
  private val organizationRoleService: OrganizationRoleService,
) {
  @GetMapping(value = ["/{projectId:[0-9]+}/transfer-options"])
  @Operation(
    summary = "Get transfer to organization options",
    description = "Returns organizations to which project can be transferred",
  )
  @RequiresProjectPermissions([Scope.PROJECT_EDIT])
  fun getTransferOptions(
    @RequestParam search: String? = "",
  ): CollectionModel<ProjectTransferOptionModel> {
    val project = projectHolder.project
    val organizations =
      organizationService.findPermittedPaged(
        PageRequest.of(0, 10),
        true,
        search,
        project.organizationOwnerId,
      )
    val options =
      organizations.content
        .map {
          ProjectTransferOptionModel(
            name = it.name,
            slug = it.slug,
            id = it.id,
          )
        }.toMutableList()
    options.sortBy { it.name }
    return CollectionModel.of(options)
  }

  @PutMapping(value = ["/{projectId:[0-9]+}/transfer-to-organization/{organizationId:[0-9]+}"])
  @Operation(summary = "Transfer project", description = "Transfers project's ownership to organization")
  @RequiresProjectPermissions([Scope.PROJECT_EDIT])
  @RequiresSuperAuthentication
  fun transferProjectToOrganization(
    @PathVariable projectId: Long,
    @PathVariable organizationId: Long,
  ) {
    organizationRoleService.checkUserCanTransferProjectToOrganization(organizationId)
    projectService.transferToOrganization(projectId, organizationId)
  }
}
