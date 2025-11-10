package io.tolgee.hateoas.invitation

import io.tolgee.api.v2.controllers.V2InvitationController
import io.tolgee.hateoas.permission.PermissionWithAgencyModelAssembler
import io.tolgee.hateoas.userAccount.SimpleUserAccountModelAssembler
import io.tolgee.model.Invitation
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.hateoas.server.mvc.linkTo
import org.springframework.stereotype.Component

@Component
class ProjectInvitationModelAssembler(
  private val permissionWithAgencyModelAssembler: PermissionWithAgencyModelAssembler,
  private val simpleUserAccountModelAssembler: SimpleUserAccountModelAssembler,
) : RepresentationModelAssemblerSupport<Invitation, ProjectInvitationModel>(
    V2InvitationController::class.java,
    ProjectInvitationModel::class.java,
  ) {
  override fun toModel(entity: Invitation): ProjectInvitationModel {
    val code =
      if (entity.permission?.agency == null) {
        entity.code
      } else {
        null
      }
    return ProjectInvitationModel(
      id = entity.id!!,
      code = code,
      type = entity.permission!!.type,
      permittedLanguageIds = entity.permission!!.translateLanguages.map { it.id },
      createdAt = entity.createdAt!!,
      invitedUserName = entity.name,
      invitedUserEmail = entity.email,
      permission = permissionWithAgencyModelAssembler.toModel(entity.permission!!),
      createdBy = entity.createdBy?.let { simpleUserAccountModelAssembler.toModel(it) },
    ).add(linkTo<V2InvitationController> { acceptInvitation(entity.code) }.withRel("accept"))
      .add(linkTo<V2InvitationController> { deleteInvitation(entity.id!!) }.withRel("delete"))
  }
}
