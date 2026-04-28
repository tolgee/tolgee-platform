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
  @Schema(
    description =
      "Whether the entry was created manually (via the add-entry dialog or TMX import) " +
        "or synced automatically from project translations. Manual entries are editable; " +
        "synced entries are read-only.",
    example = "false",
  )
  val isManual: Boolean,
  @Schema(
    description =
      "Names of the keys whose translations contribute to this synced entry. Empty for manual " +
        "entries and for synced entries whose contributing translations have all been deleted.",
  )
  val keyNames: List<String>,
  @Schema(description = "Creation timestamp")
  val createdAt: Long,
  @Schema(description = "Last update timestamp")
  val updatedAt: Long,
) : RepresentationModel<TranslationMemoryEntryModel>()
