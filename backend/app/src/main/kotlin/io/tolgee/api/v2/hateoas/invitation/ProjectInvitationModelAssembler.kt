package io.tolgee.api.v2.hateoas.invitation

import io.tolgee.api.v2.controllers.V2InvitationController
import io.tolgee.api.v2.hateoas.organization.LanguageModelAssembler
import io.tolgee.model.Invitation
import io.tolgee.security.controllers.InvitationController
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.hateoas.server.mvc.linkTo
import org.springframework.stereotype.Component

@Component
class ProjectInvitationModelAssembler(
  private val languageModelAssembler: LanguageModelAssembler
) :
  RepresentationModelAssemblerSupport<Invitation, ProjectInvitationModel>(
    InvitationController::class.java, ProjectInvitationModel::class.java
  ) {
  override fun toModel(entity: Invitation): ProjectInvitationModel {
    val languageModels = entity.permission?.languages?.map { languageModelAssembler.toModel(it) }
    return ProjectInvitationModel(
      id = entity.id!!,
      code = entity.code!!,
      type = entity.permission!!.type,
      languages = languageModels,
      createdAt = entity.createdAt!!,
      invitedUserName = entity.name,
      invitedUserEmail = entity.email
    )
      .add(linkTo<V2InvitationController> { acceptInvitation(entity.code) }.withRel("accept"))
      .add(linkTo<V2InvitationController> { deleteInvitation(entity.id!!) }.withRel("delete"))
  }
}
