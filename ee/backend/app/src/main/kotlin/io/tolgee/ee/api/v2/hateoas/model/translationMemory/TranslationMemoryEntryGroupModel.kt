package io.tolgee.ee.api.v2.hateoas.model.translationMemory

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

/**
 * A row in the TM content browser. One row per `(sourceText, origin)` where origin is manual,
 * synced (from an assigned project's translations) or virtual (computed on the fly for project
 * TMs). Manual rows are editable; synced and virtual rows are read-only.
 *
 * [entries] carries persisted rows (populated when `isManual = true` or when the row is a
 * synced entry in a shared TM). [virtualEntries] is populated only for virtual rows in a
 * project TM; they have no `id` because nothing is persisted.
 *
 * Both lists are already filtered by the requested target language(s) — if none of the
 * translations match, the list is empty but the group still appears in the page.
 */
@Relation(
  collectionRelation = "translationMemoryEntryGroups",
  itemRelation = "translationMemoryEntryGroup",
)
class TranslationMemoryEntryGroupModel(
  @Schema(description = "Source text in the TM's source language", example = "Hello world")
  val sourceText: String,
  @Schema(
    description =
      "Names of the keys whose translations currently contribute to this row. Empty for manual rows.",
  )
  val keyNames: List<String>,
  @Schema(
    description =
      "Whether the row is user-editable. True for manual rows (Add-entry dialog or TMX import); " +
        "false for synced entries and virtual rows.",
  )
  val isManual: Boolean,
  @Schema(description = "Persisted entries in this row, already filtered by the requested languages")
  val entries: List<TranslationMemoryEntryModel>,
  @Schema(description = "Virtual entries computed from project translations (project TMs only)")
  val virtualEntries: List<VirtualTranslationMemoryEntryModel>,
) : RepresentationModel<TranslationMemoryEntryGroupModel>()

/**
 * A virtual cell in a project TM's content browser. Carries no `id` since it is not persisted —
 * it's just a view of a project translation that the virtual query derived at read time.
 */
class VirtualTranslationMemoryEntryModel(
  @Schema(description = "Source text in the TM's source language", example = "Hello world")
  val sourceText: String,
  @Schema(description = "Translated target text", example = "Hallo Welt")
  val targetText: String,
  @Schema(description = "Target language tag (BCP 47)", example = "de")
  val targetLanguageTag: String,
)
