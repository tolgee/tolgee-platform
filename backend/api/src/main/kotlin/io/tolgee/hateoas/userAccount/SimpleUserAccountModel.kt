package io.tolgee.hateoas.userAccount

import io.tolgee.dtos.Avatar
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Relation(collectionRelation = "users", itemRelation = "user")
data class SimpleUserAccountModel(
  val id: Long,
  val username: String,
  var name: String?,
  var avatar: Avatar?,
  var deleted: Boolean,
) : RepresentationModel<SimpleUserAccountModel>()
