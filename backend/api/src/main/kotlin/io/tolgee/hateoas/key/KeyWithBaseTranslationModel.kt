package io.tolgee.hateoas.key

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation
import java.io.Serializable

@Suppress("unused")
@Relation(collectionRelation = "keys", itemRelation = "key")
open class KeyWithBaseTranslationModel(
  @Schema(description = "Id of key record")
  val id: Long,
  @Schema(description = "Name of key", example = "this_is_super_key")
  val name: String,
  @Schema(description = "Namespace of key", example = "homepage")
  val namespace: String?,
  @Schema(description = "Base translation", example = "This is translation")
  val baseTranslation: String?,
) : RepresentationModel<KeyWithBaseTranslationModel>(),
  Serializable
