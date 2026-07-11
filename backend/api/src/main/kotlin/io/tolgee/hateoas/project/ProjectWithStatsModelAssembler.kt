package io.tolgee.hateoas.project

import io.tolgee.api.v2.controllers.project.ProjectsController
import io.tolgee.dtos.cacheable.LanguageDto
import io.tolgee.hateoas.language.LanguageModelAssembler
import io.tolgee.hateoas.organization.SimpleOrganizationModelAssembler
import io.tolgee.hateoas.permission.ComputedPermissionModelAssembler
import io.tolgee.hateoas.permission.PermissionModelAssembler
import io.tolgee.model.views.ProjectWithStatsView
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.service.AvatarService
import io.tolgee.service.language.LanguageService
import io.tolgee.service.project.ProjectService
import io.tolgee.service.security.PermissionService
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class ProjectWithStatsModelAssembler(
  private val permissionService: PermissionService,
  private val projectService: ProjectService,
  private val languageModelAssembler: LanguageModelAssembler,
  private val avatarService: AvatarService,
  private val simpleOrganizationModelAssembler: SimpleOrganizationModelAssembler,
  private val permissionModelAssembler: PermissionModelAssembler,
  private val computedPermissionModelAssembler: ComputedPermissionModelAssembler,
  private val authenticationFacade: AuthenticationFacade,
  private val languageService: LanguageService,
) : RepresentationModelAssemblerSupport<ProjectWithStatsView, ProjectWithStatsModel>(
    ProjectsController::class.java,
    ProjectWithStatsModel::class.java,
  ) {
  override fun toModel(view: ProjectWithStatsView): ProjectWithStatsModel {
    val baseLanguage =
      languageService.getProjectLanguages(view.id).find { it.base } ?: let {
        projectService.getOrAssignBaseLanguage(view.id)
      }
    val computedPermissions =
      permissionService.computeProjectPermission(
        view.organizationRole,
        view.organizationOwner.basePermission,
        view.directPermission,
        authenticationFacade.authenticatedUserOrNull?.role,
      )

    return ProjectWithStatsModel(
      id = view.id,
      name = view.name,
      description = view.description,
      slug = view.slug,
      avatar = avatarService.getAvatarLinks(view.avatarHash),
      organizationRole = view.organizationRole,
      baseLanguage = baseLanguage.let { languageModelAssembler.toModel(LanguageDto.fromEntity(it, it.id)) },
      organizationOwner = view.organizationOwner.let { simpleOrganizationModelAssembler.toModel(it) },
      directPermission = view.directPermission?.let { permissionModelAssembler.toModel(it) },
      computedPermission = computedPermissionModelAssembler.toModel(computedPermissions),
      stats = view.stats,
      languages = view.languages.map { languageModelAssembler.toModel(it) },
      icuPlaceholders = view.icuPlaceholders,
    )
  }
}
