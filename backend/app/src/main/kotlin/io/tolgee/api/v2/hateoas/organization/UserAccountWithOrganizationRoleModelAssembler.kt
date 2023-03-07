package io.tolgee.api.v2.hateoas.organization

import io.tolgee.api.v2.controllers.V2UserController
import io.tolgee.api.v2.hateoas.project.SimpleProjectModelAssembler
import io.tolgee.model.Project
import io.tolgee.model.views.UserAccountWithOrganizationRoleView
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class UserAccountWithOrganizationRoleModelAssembler(
  private val simpleProjectModelAssembler: SimpleProjectModelAssembler
) :
  RepresentationModelAssemblerSupport<
    Pair<UserAccountWithOrganizationRoleView, List<Project>>, UserAccountWithOrganizationRoleModel
    >(
    V2UserController::class.java, UserAccountWithOrganizationRoleModel::class.java
  ) {
  override fun toModel(data: Pair<UserAccountWithOrganizationRoleView, List<Project>>):
    UserAccountWithOrganizationRoleModel {
    return UserAccountWithOrganizationRoleModel(
      id = data.first.id,
      name = data.first.name,
      username = data.first.username,
      organizationRole = data.first.organizationRole,
      projectsWithDirectPermission = data.second.map { simpleProjectModelAssembler.toModel(it) }
    )
  }
}
