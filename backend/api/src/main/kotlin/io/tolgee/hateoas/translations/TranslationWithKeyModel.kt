package io.tolgee.hateoas.translations

import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.hateoas.key.KeyModel
import io.tolgee.model.enums.TranslationState
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation
import java.io.Serializable

@Suppress("unused")
@Relation(collectionRelation = "translations", itemRelation = "translation")
open class TranslationWithKeyModel(
  @Schema(description = "Id of translation record")
  val id: Long,
  @Schema(description = "Translation text")
  val text: String?,
  @Schema(description = "State of translation")
  val state: TranslationState,
  val key: KeyModel,
) : RepresentationModel<TranslationWithKeyModel>(),
  Serializable
