package io.tolgee.api.v2.hateoas.organization

import io.tolgee.controllers.OrganizationController
import io.tolgee.model.Organization
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.hateoas.server.mvc.linkTo
import org.springframework.stereotype.Component

@Component
class OrganizationModelAssembler : RepresentationModelAssemblerSupport<Organization, OrganizationModel>(
        OrganizationController::class.java, OrganizationModel::class.java) {
    override fun toModel(entity: Organization): OrganizationModel {
        val link = linkTo<OrganizationController> { get(entity.addressPart!!) }.withSelfRel()
        return OrganizationModel(
                entity.id!!,
                entity.name!!,
                entity.addressPart!!,
                entity.description!!,
                entity.basePermissions
        ).add(link)
    }
}
