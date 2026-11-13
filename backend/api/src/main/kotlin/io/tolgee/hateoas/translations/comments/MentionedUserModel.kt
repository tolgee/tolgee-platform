package io.tolgee.hateoas.translations.comments

import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.dtos.Avatar
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation
import java.io.Serializable

@Relation(collectionRelation = "users", itemRelation = "user")
data class MentionedUserModel(
  @Schema(description = "Id of the user, used in the mention token `<@id>`")
  val id: Long,
  val name: String,
  val avatar: Avatar?,
  @Schema(description = "The user already commented on this translation")
  val inThread: Boolean = false,
) : RepresentationModel<MentionedUserModel>(),
  Serializable
