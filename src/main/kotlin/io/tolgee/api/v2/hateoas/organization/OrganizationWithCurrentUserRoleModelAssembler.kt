package io.tolgee.api.v2.hateoas.organization

import io.tolgee.controllers.OrganizationController
import io.tolgee.model.Organization
import io.tolgee.model.OrganizationRole
import io.tolgee.model.enums.OrganizationRoleType
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.hateoas.server.mvc.linkTo
import org.springframework.stereotype.Component

@Component
class OrganizationWithCurrentUserRoleModelAssembler : RepresentationModelAssemblerSupport<Array<Any>, OrganizationWithCurrentUserRoleModel>(
        OrganizationController::class.java, OrganizationWithCurrentUserRoleModel::class.java) {
    override fun toModel(organizationWithRole: Array<Any>): OrganizationWithCurrentUserRoleModel {
        val organization = organizationWithRole[0] as Organization
        val role = organizationWithRole[1] as OrganizationRole

        val link = linkTo<OrganizationController> { get(organization.addressPart!!) }.withSelfRel()
        return OrganizationWithCurrentUserRoleModel(
                organization.id!!,
                organization.name!!,
                organization.addressPart!!,
                organization.description,
                organization.basePermissions,
                role.type
        ).add(link)
    }
}
