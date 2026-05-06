package io.tolgee.ee.api.v2.hateoas.model.translationMemory

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

/**
 * A row in the TM content browser, keyed by sourceText. [entries] is the stored half
 * (user-editable); [virtualEntries] is the computed half from write-access-assigned project
 * translations (read-only).
 */
@Relation(
  collectionRelation = "translationMemoryEntryGroups",
  itemRelation = "translationMemoryEntryGroup",
)
class TranslationMemoryEntryGroupModel(
  @Schema(description = "Source text in the TM's source language", example = "Hello world")
  val sourceText: String,
  @Schema(description = "Names of the keys contributing virtual rows in this group")
  val keyNames: List<String>,
  @Schema(description = "Stored entries in this row, already filtered by the requested languages")
  val entries: List<TranslationMemoryEntryModel>,
  @Schema(description = "Virtual entries computed from write-access-assigned project translations")
  val virtualEntries: List<VirtualTranslationMemoryEntryModel>,
) : RepresentationModel<TranslationMemoryEntryGroupModel>()

class VirtualTranslationMemoryEntryModel(
  @Schema(description = "Source text in the TM's source language", example = "Hello world")
  val sourceText: String,
  @Schema(description = "Translated target text", example = "Hallo Welt")
  val targetText: String,
  @Schema(description = "Target language tag (BCP 47)", example = "de")
  val targetLanguageTag: String,
)
