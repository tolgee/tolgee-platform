package io.tolgee.hateoas.user_account

import io.tolgee.dtos.Avatar
import io.tolgee.model.UserAccount
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Relation(collectionRelation = "users", itemRelation = "user")
data class PrivateUserAccountModel(
  val id: Long,
  val username: String,
  var name: String?,
  var emailAwaitingVerification: String?,
  var mfaEnabled: Boolean,
  var avatar: Avatar?,
  var accountType: UserAccount.AccountType,
  var globalServerRole: UserAccount.Role,
  val deletable: Boolean,
  val needsSuperJwtToken: Boolean,
) : RepresentationModel<PrivateUserAccountModel>()
