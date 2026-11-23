package io.tolgee.api.v2.controllers.project

import io.tolgee.api.mcp.McpFacade
import io.tolgee.api.mcp.checkValid
import io.tolgee.dtos.request.project.CreateProjectRequest
import io.tolgee.dtos.request.project.ProjectFilters
import io.tolgee.model.enums.OrganizationRoleType
import io.tolgee.model.views.ProjectWithLanguagesView
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.service.organization.OrganizationRoleService
import io.tolgee.service.project.ProjectCreationService
import io.tolgee.service.project.ProjectService
import io.tolgee.service.security.PermissionService
import jakarta.validation.Validator
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Service
class ProjectsFacade(
  private val projectService: ProjectService,
  private val projectCreationService: ProjectCreationService,
  private val organizationRoleService: OrganizationRoleService,
  private val permissionService: PermissionService,
  private val authenticationFacade: AuthenticationFacade,
  private val validator: Validator,
) : McpFacade {
  fun findPermitted(
    pageable: Pageable,
    search: String?,
    filters: ProjectFilters? = null,
  ): Page<ProjectWithLanguagesView> =
    projectService.findPermittedInOrganizationPaged(pageable, search, filters = filters)

  fun createProject(dto: CreateProjectRequest): ProjectWithLanguagesView {
    validator.checkValid(dto)
    organizationRoleService.checkUserCanCreateProject(dto.organizationId)
    val project = projectCreationService.createProject(dto)
    if (organizationRoleService.findType(dto.organizationId) == OrganizationRoleType.MAINTAINER) {
      permissionService.grantFullAccessToProject(authenticationFacade.authenticatedUserEntity, project)
    }
    return projectService.getView(project.id)
  }
}
