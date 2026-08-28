package io.tolgee.facade

import io.tolgee.dtos.request.project.CreateProjectRequest
import io.tolgee.model.Project
import io.tolgee.model.enums.OrganizationRoleType
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.service.organization.OrganizationRoleService
import io.tolgee.service.project.ProjectCreationService
import io.tolgee.service.security.PermissionService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class ProjectCreationFacade(
  private val projectCreationService: ProjectCreationService,
  private val organizationRoleService: OrganizationRoleService,
  private val permissionService: PermissionService,
  private val authenticationFacade: AuthenticationFacade,
) {
  @Transactional
  fun createProjectAsCurrentUser(dto: CreateProjectRequest): Project {
    organizationRoleService.checkUserCanCreateProject(dto.organizationId)
    val project = projectCreationService.createProjectWithoutAuthorization(dto)
    if (organizationRoleService.findType(dto.organizationId) == OrganizationRoleType.MAINTAINER) {
      permissionService.grantFullAccessToProject(authenticationFacade.authenticatedUserEntity, project)
    }
    return project
  }
}
