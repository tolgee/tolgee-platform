package io.tolgee.hateoas.organization

import io.tolgee.api.isMfaEnabled
import io.tolgee.api.v2.controllers.V2UserController
import io.tolgee.hateoas.project.SimpleProjectModelAssembler
import io.tolgee.model.Project
import io.tolgee.model.views.UserAccountWithOrganizationRoleView
import io.tolgee.service.AvatarService
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class UserAccountWithOrganizationRoleModelAssembler(
  private val simpleProjectModelAssembler: SimpleProjectModelAssembler,
  private val avatarService: AvatarService,
) : RepresentationModelAssemblerSupport<
    Pair<UserAccountWithOrganizationRoleView, List<Project>>,
    UserAccountWithOrganizationRoleModel,
  >(
    V2UserController::class.java,
    UserAccountWithOrganizationRoleModel::class.java,
  ) {
  override fun toModel(
    data: Pair<UserAccountWithOrganizationRoleView, List<Project>>,
  ): UserAccountWithOrganizationRoleModel {
    return UserAccountWithOrganizationRoleModel(
      id = data.first.id,
      name = data.first.name,
      username = data.first.username,
      organizationRole = data.first.organizationRole,
      projectsWithDirectPermission = data.second.map { simpleProjectModelAssembler.toModel(it) },
      mfaEnabled = data.first.isMfaEnabled,
      avatar = avatarService.getAvatarLinks(data.first.avatarHash),
    )
  }
}
