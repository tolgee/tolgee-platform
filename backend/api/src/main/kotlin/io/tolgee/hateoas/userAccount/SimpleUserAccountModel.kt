package io.tolgee.hateoas.userAccount

import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.dtos.Avatar
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Relation(collectionRelation = "users", itemRelation = "user")
data class SimpleUserAccountModel(
  val id: Long,
  var name: String?,
  var avatar: Avatar?,
  var deleted: Boolean,
) : RepresentationModel<SimpleUserAccountModel>() {
  @Deprecated("A user's username (their e-mail) is only disclosed on the project members list.")
  @Suppress("unused")
  @get:Schema(
    deprecated = true,
    description =
      "Deprecated: always empty. A user's username (their e-mail) is disclosed only " +
        "on the project members list, never on author/actor references like this one.",
  )
  val username: String = ""
}
