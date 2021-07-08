package io.tolgee.api.v2.hateoas.organization

import io.tolgee.model.views.UserAccountWithOrganizationRoleView
import io.tolgee.security.controllers.UserController
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class UserAccountWithOrganizationRoleModelAssembler :
  RepresentationModelAssemblerSupport<UserAccountWithOrganizationRoleView, UserAccountWithOrganizationRoleModel>(
    UserController::class.java, UserAccountWithOrganizationRoleModel::class.java
  ) {
  override fun toModel(view: UserAccountWithOrganizationRoleView): UserAccountWithOrganizationRoleModel {
    return UserAccountWithOrganizationRoleModel(
      id = view.id,
      name = view.name,
      username = view.username,
      organizationRole = view.organizationRole
    )
  }
}
