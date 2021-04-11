package io.tolgee.api.v2.hateoas.repository

import io.tolgee.api.v2.controllers.OrganizationController
import io.tolgee.api.v2.controllers.V2RepositoriesController
import io.tolgee.api.v2.hateoas.user_account.UserAccountModelAssembler
import io.tolgee.model.views.RepositoryView
import io.tolgee.service.PermissionService
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.hateoas.server.mvc.linkTo
import org.springframework.stereotype.Component

@Component
class RepositoryModelAssembler(
        private val userAccountModelAssembler: UserAccountModelAssembler,
        private val permissionService: PermissionService
) : RepresentationModelAssemblerSupport<RepositoryView, RepositoryModel>(
        V2RepositoriesController::class.java, RepositoryModel::class.java) {
    override fun toModel(view: RepositoryView): RepositoryModel {
        val link = linkTo<V2RepositoriesController> { get(view.id) }.withSelfRel()
        return RepositoryModel(
                id = view.id,
                name = view.name,
                description = view.description,
                addressPart = view.addressPart,
                organizationOwnerAddressPart = view.organizationOwnerAddressPart,
                organizationOwnerName = view.organizationOwnerName,
                organizationOwnerBasePermissions = view.organizationBasePermissions,
                organizationRole = view.organizationRole,
                userOwner = view.userOwner?.let { userAccountModelAssembler.toModel(it) },
                directPermissions = view.directPermissions,
                computedPermissions = permissionService.computeRepositoryPermissionType(
                        view.organizationRole, view.organizationBasePermissions, view.directPermissions
                )
        ).add(link).also { model ->
            view.organizationOwnerAddressPart?.let {
                model.add(linkTo<OrganizationController> { get(it) }.withRel("organizationOwner"))
            }
        }
    }
}
