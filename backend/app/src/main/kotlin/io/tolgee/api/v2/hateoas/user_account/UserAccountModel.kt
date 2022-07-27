package io.tolgee.api.v2.hateoas.user_account

import io.tolgee.dtos.Avatar
import io.tolgee.model.UserAccount
import org.springframework.hateoas.RepresentationModel

data class UserAccountModel(
  val id: Long,
  val username: String,
  var name: String?,
  var emailAwaitingVerification: String?,
  var avatar: Avatar?,
  var globalServerRole: UserAccount.Role
) : RepresentationModel<UserAccountModel>()
