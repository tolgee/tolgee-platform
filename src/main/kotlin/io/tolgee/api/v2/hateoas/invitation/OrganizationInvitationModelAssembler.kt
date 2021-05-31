package io.tolgee.api.v2.hateoas.invitation

import io.tolgee.api.v2.hateoas.organization.OrganizationModelAssembler
import io.tolgee.model.Invitation
import io.tolgee.security.controllers.InvitationController
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.hateoas.server.mvc.linkTo
import org.springframework.stereotype.Component

@Component
class OrganizationInvitationModelAssembler(
        private val organizationModelAssembler: OrganizationModelAssembler
) : RepresentationModelAssemblerSupport<Invitation, OrganizationInvitationModel>(
        InvitationController::class.java, OrganizationInvitationModel::class.java) {
    override fun toModel(entity: Invitation): OrganizationInvitationModel {
        return OrganizationInvitationModel(
                entity.id!!,
                entity.code!!,
                entity.organizationRole!!.type!!,
                entity.createdAt!!
        )
                .add(linkTo<InvitationController> { acceptInvitation(entity.code) }.withRel("accept"))
                .add(linkTo<InvitationController> { deleteInvitation(entity.id!!) }.withRel("delete"))
    }
}
