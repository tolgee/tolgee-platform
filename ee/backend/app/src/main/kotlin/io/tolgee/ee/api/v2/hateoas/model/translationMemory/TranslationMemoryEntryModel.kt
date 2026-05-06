package io.tolgee.ee.api.v2.hateoas.model.translationMemory

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Relation(collectionRelation = "translationMemoryEntries", itemRelation = "translationMemoryEntry")
class TranslationMemoryEntryModel(
  val id: Long,
  @Schema(description = "Source text in the TM's source language", example = "Hello world")
  val sourceText: String,
  @Schema(description = "Translated target text", example = "Hallo Welt")
  val targetText: String,
  @Schema(description = "Target language tag (BCP 47)", example = "de")
  val targetLanguageTag: String,
  @Schema(description = "Creation timestamp")
  val createdAt: Long,
  @Schema(description = "Last update timestamp")
  val updatedAt: Long,
) : RepresentationModel<TranslationMemoryEntryModel>()
