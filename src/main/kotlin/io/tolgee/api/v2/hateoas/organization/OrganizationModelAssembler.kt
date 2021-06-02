package io.tolgee.api.v2.hateoas.organization

import io.tolgee.api.v2.controllers.OrganizationController
import io.tolgee.model.views.OrganizationView
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.hateoas.server.mvc.linkTo
import org.springframework.stereotype.Component

@Component
class OrganizationModelAssembler : RepresentationModelAssemblerSupport<OrganizationView, OrganizationModel>(
        OrganizationController::class.java, OrganizationModel::class.java) {
    override fun toModel(view: OrganizationView): OrganizationModel {
        val link = linkTo<OrganizationController> { get(view.slug) }.withSelfRel()
        return OrganizationModel(
                view.id,
                view.name,
                view.slug,
                view.description,
                view.basePermissions,
                view.currentUserRole
        ).add(link)
    }
}
