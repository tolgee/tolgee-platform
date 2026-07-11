package io.tolgee.ee.service.translationMemory.tmx

/**
 * Result of parsing a TMX file with [TmxParser].
 *
 * @property entries Parsed entries ready to be persisted.
 * @property skippedOversize Number of would-have-been entries dropped because their source or
 *   target text exceeded [io.tolgee.model.translationMemory.TranslationMemoryEntry.MAX_TEXT_LENGTH].
 *   The import response folds this into [io.tolgee.ee.data.translationMemory.TmxImportResult.skipped]
 *   so users see that something was dropped instead of having long segments disappear silently.
 */
data class TmxParseResult(
  val entries: List<TmxParsedEntry>,
  val skippedOversize: Int,
)
