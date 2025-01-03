package io.tolgee.hateoas.invitation

import io.tolgee.api.v2.controllers.V2InvitationController
import io.tolgee.hateoas.userAccount.SimpleUserAccountModelAssembler
import io.tolgee.model.Invitation
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class PublicInvitationModelAssembler(
  private val simpleUserAccountModelAssembler: SimpleUserAccountModelAssembler,
) : RepresentationModelAssemblerSupport<Invitation, PublicInvitationModel>(
    V2InvitationController::class.java,
    PublicInvitationModel::class.java,
  ) {
  override fun toModel(entity: Invitation): PublicInvitationModel {
    return PublicInvitationModel(
      id = entity.id!!,
      code = entity.code,
      projectName = entity.permission?.project?.name,
      organizationName = entity.organizationRole?.organization?.name,
      createdBy = entity.createdBy?.let { simpleUserAccountModelAssembler.toModel(it) },
    )
  }
}
