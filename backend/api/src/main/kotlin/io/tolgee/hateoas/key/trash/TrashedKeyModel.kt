package io.tolgee.hateoas.key.trash

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation
import java.io.Serializable
import java.util.Date

@Suppress("unused")
@Relation(collectionRelation = "keys", itemRelation = "key")
class TrashedKeyModel(
  @Schema(description = "Id of key record")
  val id: Long,
  @Schema(description = "Name of key", example = "this_is_super_key")
  val name: String,
  @Schema(description = "Namespace of key", example = "homepage")
  val namespace: String?,
  @Schema(description = "When the key was deleted")
  val deletedAt: Date,
  @Schema(description = "When the key will be permanently deleted")
  val permanentDeleteAt: Date,
) : RepresentationModel<TrashedKeyModel>(),
  Serializable
