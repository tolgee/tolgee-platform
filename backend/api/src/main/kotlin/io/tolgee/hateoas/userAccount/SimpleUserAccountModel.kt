package io.tolgee.hateoas.userAccount

import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.api.USERNAME_FIELD_DEPRECATION
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
  @Deprecated(USERNAME_FIELD_DEPRECATION)
  @Suppress("unused")
  @get:Schema(deprecated = true, description = USERNAME_FIELD_DEPRECATION)
  val username: String = ""
}
