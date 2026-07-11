package io.tolgee.ee.api.v2.hateoas.model.translationMemory

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

/**
 * One row in the TM content browser. Cells are unified — clients don't need to distinguish
 * "stored" from "virtual" up front; [editable] indicates whether the row itself can be
 * mutated (only manually-managed rows are). When the row mirrors a project key,
 * [keyName] / [projectId] / [projectName] identify the source so the UI can surface a link
 * back to the project. Pagination is row-level — a single source text can produce several
 * rows when both manual entries and one or more project keys share it.
 */
@Relation(
  collectionRelation = "translationMemoryRows",
  itemRelation = "translationMemoryRow",
)
class TranslationMemoryRowModel(
  @Schema(description = "Source text in the TM's source language", example = "Hello world")
  val sourceText: String,
  @Schema(
    description =
      "Whether the row can be edited (manual TM entries) or is read-only " +
        "(mirrored from a project key — change it in the project).",
    example = "true",
  )
  val editable: Boolean,
  @Schema(description = "Cells of this row, already filtered by the requested languages")
  val cells: List<TranslationMemoryRowCellModel>,
  @Schema(description = "Originating project key name when the row mirrors a project key", example = "greeting.hello")
  val keyName: String?,
  @Schema(description = "Originating project id when the row mirrors a project key", example = "42")
  val projectId: Long?,
  @Schema(description = "Originating project name when the row mirrors a project key", example = "My project")
  val projectName: String?,
) : RepresentationModel<TranslationMemoryRowModel>()

class TranslationMemoryRowCellModel(
  @Schema(description = "Translated target text", example = "Hallo Welt")
  val targetText: String,
  @Schema(description = "Target language tag (BCP 47)", example = "de")
  val targetLanguageTag: String,
  @Schema(
    description =
      "Id of the underlying TM entry when the cell is editable. Absent for " +
        "read-only cells mirrored from project translations.",
    example = "12345",
  )
  val entryId: Long?,
)
