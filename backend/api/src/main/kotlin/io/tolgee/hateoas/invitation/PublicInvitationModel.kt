package io.tolgee.hateoas.invitation

import io.tolgee.hateoas.userAccount.SimpleUserAccountModel
import org.springframework.hateoas.RepresentationModel

class PublicInvitationModel(
  val id: Long,
  val code: String,
  val createdBy: SimpleUserAccountModel?,
  val projectName: String?,
  val organizationName: String?,
) : RepresentationModel<PublicInvitationModel>()
