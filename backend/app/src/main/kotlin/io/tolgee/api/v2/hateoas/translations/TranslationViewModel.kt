package io.tolgee.api.v2.hateoas.translations

import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.model.enums.TranslationState
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Suppress("unused")
@Relation(collectionRelation = "translations", itemRelation = "translation")
open class TranslationViewModel(
  @Schema(description = "Id of translation record")
  val id: Long,

  @Schema(description = "Translation text")
  val text: String?,

  @Schema(description = "State of translation")
  val state: TranslationState,

  @Schema(description = "Count of translation comments")
  val commentCount: Long

) : RepresentationModel<TranslationViewModel>()
