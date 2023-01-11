package io.tolgee.api.v2.hateoas.user_account

import io.tolgee.api.v2.controllers.V2UserController
import io.tolgee.api.v2.hateoas.UserPermissionModel
import io.tolgee.model.views.UserAccountInProjectWithLanguagesView
import io.tolgee.service.security.PermissionService
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class UserAccountInProjectModelAssembler(
  val permissionService: PermissionService
) : RepresentationModelAssemblerSupport<UserAccountInProjectWithLanguagesView, UserAccountInProjectModel>(
  V2UserController::class.java, UserAccountInProjectModel::class.java
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
