package io.tolgee.hateoas.invitation

import io.tolgee.api.v2.controllers.V2InvitationController
import io.tolgee.hateoas.permission.PermissionModelAssembler
import io.tolgee.model.Invitation
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.hateoas.server.mvc.linkTo
import org.springframework.stereotype.Component

@Component
class ProjectInvitationModelAssembler(
  private val permissionModelAssembler: PermissionModelAssembler,
) :
  RepresentationModelAssemblerSupport<Invitation, ProjectInvitationModel>(
      V2InvitationController::class.java,
      ProjectInvitationModel::class.java,
    ) {
  override fun toModel(entity: Invitation): ProjectInvitationModel {
    return ProjectInvitationModel(
      id = entity.id!!,
      code = entity.code,
      type = entity.permission!!.type,
      permittedLanguageIds = entity.permission!!.translateLanguages.map { it.id },
      createdAt = entity.createdAt!!,
      invitedUserName = entity.name,
      invitedUserEmail = entity.email,
      permission = permissionModelAssembler.toModel(entity.permission!!),
    )
      .add(linkTo<V2InvitationController> { acceptInvitation(entity.code) }.withRel("accept"))
      .add(linkTo<V2InvitationController> { deleteInvitation(entity.id!!) }.withRel("delete"))
  }
}
