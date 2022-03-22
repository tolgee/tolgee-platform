package io.tolgee.api.v2.hateoas.user_account

import io.tolgee.api.v2.hateoas.UserPermissionModel
import io.tolgee.model.views.UserAccountInProjectWithLanguagesView
import io.tolgee.security.controllers.UserController
import io.tolgee.service.PermissionService
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class UserAccountInProjectModelAssembler(
  val permissionService: PermissionService
) : RepresentationModelAssemblerSupport<UserAccountInProjectWithLanguagesView, UserAccountInProjectModel>(
  UserController::class.java, UserAccountInProjectModel::class.java
) {
  override fun toModel(view: UserAccountInProjectWithLanguagesView): UserAccountInProjectModel {
    return UserAccountInProjectModel(
      view.id,
      view.username,
      view.name,
      view.organizationRole,
      view.organizationBasePermissions,
      view.directPermissions,
      UserPermissionModel(
        type = permissionService.computeProjectPermissionType(
          view.organizationRole, view.organizationBasePermissions, view.directPermissions, null
        ).type!!,
        permittedLanguageIds = view.permittedLanguageIds
      ),
    )
  }
}
