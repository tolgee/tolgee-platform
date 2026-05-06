package io.tolgee.ee.service.translationMemory

import io.tolgee.model.translationMemory.TranslationMemoryEntry

/**
 * One row in the TM content browser, keyed by [sourceText]. [entries] is the stored half
 * (user-created entries the row owns); [virtualEntries] is the live half (computed from
 * write-access-assigned project translations). A row may have either or both.
 */
data class TranslationMemoryEntryGroup(
  val sourceText: String,
  val keyNames: List<String>,
  val entries: List<TranslationMemoryEntry>,
  val virtualEntries: List<VirtualEntry>,
)
