package io.tolgee.hateoas.userAccount

import io.tolgee.dtos.Avatar
import io.tolgee.model.UserAccount
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

/** Discloses the user's e-mail; an allowlisted disclosure surface — see `UsernameDisclosureGuard.allowlistedModelNames`. */
@Relation(collectionRelation = "users", itemRelation = "user")
data class UserAccountModel(
  val id: Long,
  val username: String,
  val name: String?,
  val emailAwaitingVerification: String?,
  val avatar: Avatar?,
  val globalServerRole: UserAccount.Role,
  val mfaEnabled: Boolean,
  val deleted: Boolean,
  val disabled: Boolean,
) : RepresentationModel<UserAccountModel>()
