package io.tolgee.hateoas.userAccount

import io.tolgee.dtos.Avatar
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.ThirdPartyAuthType
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Relation(collectionRelation = "users", itemRelation = "user")
data class PrivateUserAccountModel(
  val id: Long,
  val username: String,
  var name: String?,
  val domain: String?,
  var emailAwaitingVerification: String?,
  var mfaEnabled: Boolean,
  var avatar: Avatar?,
  var accountType: UserAccount.AccountType,
  var thirdPartyAuthType: ThirdPartyAuthType?,
  var globalServerRole: UserAccount.Role,
  val deletable: Boolean,
  val needsSuperJwtToken: Boolean,
) : RepresentationModel<PrivateUserAccountModel>()
