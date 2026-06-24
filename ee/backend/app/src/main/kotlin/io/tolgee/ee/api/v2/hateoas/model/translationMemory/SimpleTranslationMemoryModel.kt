package io.tolgee.ee.api.v2.hateoas.model.translationMemory

import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.model.translationMemory.TranslationMemoryType
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Relation(collectionRelation = "translationMemories", itemRelation = "translationMemory")
class SimpleTranslationMemoryModel(
  val id: Long,
  val name: String,
  @Schema(
    description = "Source language tag of the translation memory",
    example = "en",
  )
  val sourceLanguageTag: String,
  val type: TranslationMemoryType,
) : RepresentationModel<SimpleTranslationMemoryModel>()
