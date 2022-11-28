package io.tolgee.api.v2.hateoas.user_account

import io.tolgee.api.v2.controllers.V2UserController
import io.tolgee.api.v2.hateoas.permission.PermissionModel
import io.tolgee.api.v2.hateoas.permission.PermissionModelAssembler
import io.tolgee.model.views.UserAccountInProjectWithLanguagesView
import io.tolgee.service.security.PermissionService
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class UserAccountInProjectModelAssembler(
  private val permissionService: PermissionService,
  private val permissionModelAssembler: PermissionModelAssembler
) : RepresentationModelAssemblerSupport<UserAccountInProjectWithLanguagesView, UserAccountInProjectModel>(
  V2UserController::class.java, UserAccountInProjectModel::class.java
) {
  override fun toModel(view: UserAccountInProjectWithLanguagesView): UserAccountInProjectModel {
    val computedPermissions = permissionService.computeProjectPermissionType(
      view.organizationRole,
      view.organizationBasePermissions.scopes,
      view.directPermissions?.scopes,
      view.directPermissions?.languages?.map { it.id }?.toMutableSet()
    )
    return UserAccountInProjectModel(
      view.id,
      view.username,
      view.name,
      view.organizationRole,
      permissionModelAssembler.toModel(view.organizationBasePermissions),
      view.directPermissions?.let { permissionModelAssembler.toModel(it) },
      PermissionModel(
        scopes = computedPermissions.scopes!!,
        permittedLanguageIds = view.permittedLanguageIds
      ),
    )
  }
}
