package io.tolgee.api.v2.hateoas.project

import io.tolgee.api.v2.controllers.OrganizationController
import io.tolgee.api.v2.controllers.V2ProjectsController
import io.tolgee.api.v2.hateoas.UserPermissionModel
import io.tolgee.api.v2.hateoas.organization.LanguageModelAssembler
import io.tolgee.api.v2.hateoas.user_account.UserAccountModelAssembler
import io.tolgee.model.views.ProjectWithStatsView
import io.tolgee.service.AvatarService
import io.tolgee.service.PermissionService
import io.tolgee.service.ProjectService
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.hateoas.server.mvc.linkTo
import org.springframework.stereotype.Component

@Component
class ProjectWithStatsModelAssembler(
  private val userAccountModelAssembler: UserAccountModelAssembler,
  private val permissionService: PermissionService,
  private val projectService: ProjectService,
  private val languageModelAssembler: LanguageModelAssembler,
  private val avatarService: AvatarService
) : RepresentationModelAssemblerSupport<ProjectWithStatsView, ProjectWithStatsModel>(
  V2ProjectsController::class.java, ProjectWithStatsModel::class.java
) {
  override fun toModel(view: ProjectWithStatsView): ProjectWithStatsModel {
    val link = linkTo<V2ProjectsController> { get(view.id) }.withSelfRel()
    val baseLanguage = view.baseLanguage ?: let {
      projectService.getOrCreateBaseLanguage(view.id)
    }
    return ProjectWithStatsModel(
      id = view.id,
      name = view.name,
      description = view.description,
      slug = view.slug,
      avatar = avatarService.getAvatarLinks(view.avatarHash),
      organizationOwnerSlug = view.organizationOwnerSlug,
      organizationOwnerName = view.organizationOwnerName,
      organizationOwnerBasePermissions = view.organizationBasePermissions,
      organizationRole = view.organizationRole,
      baseLanguage = baseLanguage?.let { languageModelAssembler.toModel(baseLanguage) },
      userOwner = view.userOwner?.let { userAccountModelAssembler.toModel(it) },
      directPermissions = view.directPermissions,
      computedPermissions = UserPermissionModel(
        type = permissionService.computeProjectPermissionType(
          view.organizationRole, view.organizationBasePermissions, view.directPermissions, null
        ).type!!,
        permittedLanguageIds = view.permittedLanguageIds
      ),
      stats = view.stats,
      languages = view.languages.map { languageModelAssembler.toModel(it) },
    ).add(link).also { model ->
      view.organizationOwnerSlug?.let {
        model.add(linkTo<OrganizationController> { get(it) }.withRel("organizationOwner"))
      }
    }
  }
}
