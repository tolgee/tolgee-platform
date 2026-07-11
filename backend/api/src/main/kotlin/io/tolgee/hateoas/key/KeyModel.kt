package io.tolgee.hateoas.key

import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.api.IKeyModel
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation
import java.io.Serializable

@Suppress("unused")
@Relation(collectionRelation = "keys", itemRelation = "key")
open class KeyModel(
  @Schema(description = "Id of key record")
  override val id: Long,
  @Schema(description = "Name of key", example = "this_is_super_key")
  override val name: String,
  @Schema(description = "Namespace of key", example = "homepage")
  override val namespace: String?,
  @Schema(
    description = "Description of key",
    example = "This key is used on homepage. It's a label of sign up button.",
  )
  override val description: String?,
  @Schema(description = "Custom values of the key")
  override val custom: Map<String, Any?>?,
  @Schema(description = "Maximum character limit for translations of this key")
  val maxCharLimit: Int?,
  @Schema(description = "Branch of key", example = "dev")
  val branch: String?,
) : RepresentationModel<KeyModel>(),
  Serializable,
  IKeyModel
