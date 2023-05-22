package io.tolgee.api.v2.hateoas.project

import io.tolgee.api.v2.controllers.V2ProjectsController
import io.tolgee.api.v2.controllers.organization.OrganizationController
import io.tolgee.api.v2.hateoas.language.LanguageModelAssembler
import io.tolgee.api.v2.hateoas.organization.SimpleOrganizationModelAssembler
import io.tolgee.api.v2.hateoas.permission.ComputedPermissionModelAssembler
import io.tolgee.api.v2.hateoas.permission.PermissionModelAssembler
import io.tolgee.dtos.ComputedPermissionDto
import io.tolgee.model.UserAccount
import io.tolgee.model.views.ProjectWithLanguagesView
import io.tolgee.model.views.ProjectWithStatsView
import io.tolgee.security.AuthenticationFacade
import io.tolgee.service.AvatarService
import io.tolgee.service.project.ProjectService
import io.tolgee.service.security.PermissionService
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.hateoas.server.mvc.linkTo
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
  private val authenticationFacade: AuthenticationFacade
) : RepresentationModelAssemblerSupport<ProjectWithStatsView, ProjectWithStatsModel>(
  V2ProjectsController::class.java, ProjectWithStatsModel::class.java
) {
  override fun toModel(view: ProjectWithStatsView): ProjectWithStatsModel {
    val link = linkTo<V2ProjectsController> { get(view.id) }.withSelfRel()
    val baseLanguage = view.baseLanguage ?: let {
      projectService.getOrCreateBaseLanguage(view.id)
    }
    val computedPermissions = getComputedPermissions(view)

    return ProjectWithStatsModel(
      id = view.id,
      name = view.name,
      description = view.description,
      slug = view.slug,
      avatar = avatarService.getAvatarLinks(view.avatarHash),
      organizationRole = view.organizationRole,
      baseLanguage = baseLanguage?.let { languageModelAssembler.toModel(baseLanguage) },
      organizationOwner = view.organizationOwner.let { simpleOrganizationModelAssembler.toModel(it) },
      directPermission = view.directPermission?.let { permissionModelAssembler.toModel(it) },
      computedPermission = computedPermissionModelAssembler.toModel(computedPermissions),
      stats = view.stats,
      languages = view.languages.map { languageModelAssembler.toModel(it) },
    ).add(link).also { model ->
      view.organizationOwner.slug.let {
        model.add(linkTo<OrganizationController> { get(it) }.withRel("organizationOwner"))
      }
    }
  }

  private fun getComputedPermissions(view: ProjectWithLanguagesView): ComputedPermissionDto {
    if (authenticationFacade.userAccountOrNull?.role == UserAccount.Role.ADMIN) {
      return ComputedPermissionDto.SERVER_ADMIN
    }

    return permissionService.computeProjectPermission(
      view.organizationRole,
      view.organizationOwner.basePermission,
      view.directPermission,
      UserAccount.Role.USER
    )
  }
}
