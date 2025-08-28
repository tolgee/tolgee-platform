package io.tolgee.hateoas.userAccount

import io.tolgee.api.v2.controllers.V2UserController
import io.tolgee.hateoas.permission.ComputedPermissionModelAssembler
import io.tolgee.hateoas.permission.PermissionModelAssembler
import io.tolgee.hateoas.permission.PermissionWithAgencyModelAssembler
import io.tolgee.model.UserAccount
import io.tolgee.model.views.ExtendedUserAccountInProject
import io.tolgee.service.AvatarService
import io.tolgee.service.security.PermissionService
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class UserAccountInProjectModelAssembler(
  private val permissionService: PermissionService,
  private val permissionModelAssembler: PermissionModelAssembler,
  private val computedPermissionModelAssembler: ComputedPermissionModelAssembler,
  private val avatarService: AvatarService,
  private val permissionWithAgencyModelAssembler: PermissionWithAgencyModelAssembler,
) : RepresentationModelAssemblerSupport<ExtendedUserAccountInProject, UserAccountInProjectModel>(
    V2UserController::class.java,
    UserAccountInProjectModel::class.java,
  ) {
  override fun toModel(view: ExtendedUserAccountInProject): UserAccountInProjectModel {
    val computedPermissions =
      permissionService.computeProjectPermission(
        view.organizationRole,
        view.organizationBasePermission,
        view.directPermission,
        UserAccount.Role.USER,
      )
    val avatar = avatarService.getAvatarLinks(view.avatarHash)
    return UserAccountInProjectModel(
      id = view.id,
      username = view.username,
      name = view.name,
      organizationRole = view.organizationRole,
      organizationBasePermission = permissionModelAssembler.toModel(view.organizationBasePermission),
      directPermission = view.directPermission?.let { permissionWithAgencyModelAssembler.toModel(it) },
      computedPermission = computedPermissionModelAssembler.toModel(computedPermissions),
      mfaEnabled = view.mfaEnabled,
      avatar = avatar,
    )
  }
}
