package io.tolgee.hateoas.project

import io.tolgee.api.v2.controllers.ProjectsController
import io.tolgee.api.v2.controllers.organization.OrganizationController
import io.tolgee.dtos.ComputedPermissionDto
import io.tolgee.dtos.cacheable.LanguageDto
import io.tolgee.hateoas.language.LanguageModelAssembler
import io.tolgee.hateoas.organization.SimpleOrganizationModelAssembler
import io.tolgee.hateoas.permission.ComputedPermissionModelAssembler
import io.tolgee.hateoas.permission.PermissionModelAssembler
import io.tolgee.model.UserAccount
import io.tolgee.model.views.ProjectWithLanguagesView
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.service.AvatarService
import io.tolgee.service.project.ProjectService
import io.tolgee.service.security.PermissionService
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.hateoas.server.mvc.linkTo
import org.springframework.stereotype.Component

@Component
class ProjectModelAssembler(
  private val permissionService: PermissionService,
  private val projectService: ProjectService,
  private val languageModelAssembler: LanguageModelAssembler,
  private val avatarService: AvatarService,
  private val simpleOrganizationModelAssembler: SimpleOrganizationModelAssembler,
  private val permissionModelAssembler: PermissionModelAssembler,
  private val computedPermissionModelAssembler: ComputedPermissionModelAssembler,
  private val authenticationFacade: AuthenticationFacade,
) : RepresentationModelAssemblerSupport<ProjectWithLanguagesView, ProjectModel>(
    ProjectsController::class.java,
    ProjectModel::class.java,
  ) {
  override fun toModel(view: ProjectWithLanguagesView): ProjectModel {
    val link = linkTo<ProjectsController> { get(view.id) }.withSelfRel()
    val baseLanguage =
      view.baseLanguage ?: let {
        projectService.getOrAssignBaseLanguage(view.id)
      }

    val computedPermissions = getComputedPermissions(view)

    return ProjectModel(
      id = view.id,
      name = view.name,
      description = view.description,
      slug = view.slug,
      avatar = avatarService.getAvatarLinks(view.avatarHash),
      organizationRole = view.organizationRole,
      organizationOwner = view.organizationOwner.let { simpleOrganizationModelAssembler.toModel(it) },
      baseLanguage = baseLanguage.let { languageModelAssembler.toModel(LanguageDto.fromEntity(it, it.id)) },
      directPermission = view.directPermission?.let { permissionModelAssembler.toModel(it) },
      computedPermission = computedPermissionModelAssembler.toModel(computedPermissions),
      icuPlaceholders = view.icuPlaceholders,
    ).add(link).also { model ->
      model.add(linkTo<OrganizationController> { get(view.organizationOwner.slug) }.withRel("organizationOwner"))
    }
  }

  private fun getComputedPermissions(view: ProjectWithLanguagesView): ComputedPermissionDto {
    return permissionService.computeProjectPermission(
      view.organizationRole,
      view.organizationOwner.basePermission,
      view.directPermission,
      UserAccount.Role.USER,
    ).getAdminPermissions(authenticationFacade.authenticatedUserOrNull?.role)
  }
}
