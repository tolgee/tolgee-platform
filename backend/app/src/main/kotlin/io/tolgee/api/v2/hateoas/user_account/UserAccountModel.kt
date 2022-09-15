package io.tolgee.api.v2.hateoas.user_account

import io.tolgee.dtos.Avatar
import io.tolgee.model.UserAccount
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation
import java.util.*

@Relation(collectionRelation = "users", itemRelation = "user")
data class UserAccountModel(
  val id: Long,
  val username: String,
  var name: String?,
  var emailAwaitingVerification: String?,
  var avatar: Avatar?,
  var globalServerRole: UserAccount.Role,
  var createdAt: Date
) : RepresentationModel<UserAccountModel>()
