package io.tolgee.ee.api.v2.hateoas.model.translationMemory

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

/**
 * One row in the TM content browser. Stored rows carry user-editable cells in [entries];
 * virtual rows carry read-only cells in [virtualEntries] plus the originating project
 * identification. Pagination is row-level — a single source text can produce several rows.
 */
@Relation(
  collectionRelation = "translationMemoryRows",
  itemRelation = "translationMemoryRow",
)
class TranslationMemoryRowModel(
  @Schema(description = "Source text in the TM's source language", example = "Hello world")
  val sourceText: String,
  @Schema(description = "Row kind — STORED is user-managed; VIRTUAL is computed from a project translation", example = "STORED")
  val kind: Kind,
  @Schema(description = "Stored cells of this row, already filtered by the requested languages")
  val entries: List<TranslationMemoryEntryModel>,
  @Schema(description = "Virtual cells of this row, already filtered by the requested languages")
  val virtualEntries: List<VirtualTranslationMemoryEntryModel>,
  @Schema(description = "Originating project key name (virtual rows only)", example = "greeting.hello")
  val keyName: String?,
  @Schema(description = "Originating project id (virtual rows only)", example = "42")
  val projectId: Long?,
  @Schema(description = "Originating project name (virtual rows only)", example = "My project")
  val projectName: String?,
) : RepresentationModel<TranslationMemoryRowModel>() {
  enum class Kind {
    STORED,
    VIRTUAL,
  }
}

class VirtualTranslationMemoryEntryModel(
  @Schema(description = "Source text in the TM's source language", example = "Hello world")
  val sourceText: String,
  @Schema(description = "Translated target text", example = "Hallo Welt")
  val targetText: String,
  @Schema(description = "Target language tag (BCP 47)", example = "de")
  val targetLanguageTag: String,
  @Schema(description = "Id of the project the virtual row originates from", example = "42")
  val projectId: Long,
  @Schema(description = "Name of the project the virtual row originates from", example = "My project")
  val projectName: String,
  @Schema(description = "Name of the project key the virtual row originates from", example = "greeting.hello")
  val keyName: String,
)
