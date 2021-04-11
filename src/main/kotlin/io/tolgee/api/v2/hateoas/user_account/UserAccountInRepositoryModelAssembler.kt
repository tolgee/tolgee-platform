package io.tolgee.api.v2.hateoas.user_account

import io.tolgee.model.views.UserAccountInRepositoryView
import io.tolgee.security.controllers.UserController
import io.tolgee.service.PermissionService
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class UserAccountInRepositoryModelAssembler(
        val permissionService: PermissionService
) : RepresentationModelAssemblerSupport<UserAccountInRepositoryView, UserAccountInRepositoryModel>(
        UserController::class.java, UserAccountInRepositoryModel::class.java) {
    override fun toModel(view: UserAccountInRepositoryView): UserAccountInRepositoryModel {
        return UserAccountInRepositoryModel(
                view.id,
                view.username,
                view.name,
                view.organizationRole,
                view.organizationBasePermissions,
                view.directPermissions,
                permissionService.computeRepositoryPermissionType(view.organizationRole, view.organizationBasePermissions, view.directPermissions)!!
        )
    }
}
