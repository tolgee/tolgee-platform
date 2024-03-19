package io.tolgee.hateoas.translations

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Suppress("unused")
@Relation(collectionRelation = "keys", itemRelation = "key")
open class SetTranslationsResponseModel(
  @Schema(description = "Id of key record")
  val keyId: Long,
  @Schema(description = "Name of key", example = "this_is_super_key")
  val keyName: String,
  @Schema(description = "The namespace of the key", example = "homepage")
  val keyNamespace: String?,
  val keyIsPlural: Boolean,
  @Schema(
    description = "Translations object containing values updated in this request",
    example = "{\"en\": {\"id\": 100000003, \"text\": \"This is super translation!\" }}",
  )
  val translations: Map<String, TranslationModel>,
) : RepresentationModel<SetTranslationsResponseModel>()
