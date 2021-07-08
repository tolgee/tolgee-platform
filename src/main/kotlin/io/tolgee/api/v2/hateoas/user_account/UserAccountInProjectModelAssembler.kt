package io.tolgee.api.v2.hateoas.user_account

import io.tolgee.model.views.UserAccountInProjectView
import io.tolgee.security.controllers.UserController
import io.tolgee.service.PermissionService
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class UserAccountInProjectModelAssembler(
  val permissionService: PermissionService
) : RepresentationModelAssemblerSupport<UserAccountInProjectView, UserAccountInProjectModel>(
  UserController::class.java, UserAccountInProjectModel::class.java
) {
  override fun toModel(view: UserAccountInProjectView): UserAccountInProjectModel {
    return UserAccountInProjectModel(
      view.id,
      view.username,
      view.name,
      view.organizationRole,
      view.organizationBasePermissions,
      view.directPermissions,
      permissionService.computeProjectPermissionType(
        view.organizationRole, view.organizationBasePermissions, view.directPermissions
      )!!
    )
  }
}
