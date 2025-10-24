package io.tolgee.hateoas.userAccount

import io.tolgee.dtos.Avatar
import io.tolgee.model.UserAccount
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation
import java.util.Date

@Relation(collectionRelation = "users", itemRelation = "user")
data class UserAccountAdministrationModel(
  val id: Long,
  val username: String,
  val name: String?,
  val emailAwaitingVerification: String?,
  val avatar: Avatar?,
  val globalServerRole: UserAccount.Role,
  val mfaEnabled: Boolean,
  val deleted: Boolean,
  val disabled: Boolean,
  val lastActivity: Date?,
) : RepresentationModel<UserAccountAdministrationModel>()
