package io.tolgee.api.v2.hateoas.project

import io.tolgee.api.v2.controllers.OrganizationController
import io.tolgee.api.v2.controllers.V2RepositoriesController
import io.tolgee.api.v2.hateoas.user_account.UserAccountModelAssembler
import io.tolgee.model.views.ProjectView
import io.tolgee.service.PermissionService
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.hateoas.server.mvc.linkTo
import org.springframework.stereotype.Component

@Component
class ProjectModelAssembler(
        private val userAccountModelAssembler: UserAccountModelAssembler,
        private val permissionService: PermissionService
) : RepresentationModelAssemblerSupport<ProjectView, ProjectModel>(
        V2RepositoriesController::class.java, ProjectModel::class.java) {
    override fun toModel(view: ProjectView): ProjectModel {
        val link = linkTo<V2RepositoriesController> { get(view.id) }.withSelfRel()
        return ProjectModel(
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
                computedPermissions = permissionService.computeProjectPermissionType(
                        view.organizationRole, view.organizationBasePermissions, view.directPermissions
                )
        ).add(link).also { model ->
            view.organizationOwnerAddressPart?.let {
                model.add(linkTo<OrganizationController> { get(it) }.withRel("organizationOwner"))
            }
        }
    }
}
