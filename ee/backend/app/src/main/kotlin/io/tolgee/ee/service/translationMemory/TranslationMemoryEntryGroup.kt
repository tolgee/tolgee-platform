package io.tolgee.ee.service.translationMemory

import io.tolgee.model.translationMemory.TranslationMemoryEntry

/**
 * One row visible in the TM content browser. Rows are formed by `(sourceText, origin)` where
 * origin is either stored-with-`is_manual=true`, stored-with-`is_manual=false`, or virtual
 * (for project TMs, computed from translations on the fly).
 *
 * - [entries] carries persisted rows; populated for stored groups and empty for virtual groups.
 * - [virtualEntries] is the inverse — populated only for virtual groups in a project TM.
 * - [keyNames] aggregates every key that contributes to this row; empty for manual rows.
 * - [isManual] controls per-row editability in the UI. Virtual rows set this to `false`.
 */
data class TranslationMemoryEntryGroup(
  val sourceText: String,
  val keyNames: List<String>,
  val isManual: Boolean,
  val entries: List<TranslationMemoryEntry>,
  val virtualEntries: List<VirtualEntry>,
)
