package io.tolgee.ee.service.translationMemory

import io.tolgee.ee.data.translationMemory.TmxImportResult
import io.tolgee.ee.service.translationMemory.tmx.TmxExporter
import io.tolgee.ee.service.translationMemory.tmx.TmxParsedEntry
import io.tolgee.ee.service.translationMemory.tmx.TmxParser
import io.tolgee.model.translationMemory.TranslationMemory
import io.tolgee.model.translationMemory.TranslationMemoryEntry
import io.tolgee.model.translationMemory.TranslationMemoryType
import io.tolgee.repository.translationMemory.TranslationMemoryEntryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.InputStream

@Service
class TranslationMemoryTmxService(
  private val translationMemoryEntryRepository: TranslationMemoryEntryRepository,
  private val translationMemoryEntryManagementService: TranslationMemoryEntryManagementService,
) {
  fun exportTmx(tm: TranslationMemory): ByteArray {
    val entries = loadEntriesForExport(tm)
    return TmxExporter(tm.sourceLanguageTag, entries).export()
  }

  /**
   * SHARED TMs ship their stored rows. PROJECT TMs hold no stored rows by design — their content
   * is computed virtually from the assigned project's translations, so the export hydrates that
   * virtual view to match what the content browser shows.
   */
  private fun loadEntriesForExport(tm: TranslationMemory): List<TranslationMemoryEntry> {
    if (tm.type == TranslationMemoryType.PROJECT) {
      return translationMemoryEntryManagementService.findEntriesForProjectTmExport(tm)
    }
    return translationMemoryEntryRepository.findByTranslationMemoryId(tm.id)
  }

  @Transactional
  fun importTmx(
    tm: TranslationMemory,
    inputStream: InputStream,
    overrideExisting: Boolean,
  ): TmxImportResult {
    val parsed = TmxParser(tm.sourceLanguageTag).parse(inputStream)
    if (parsed.isEmpty()) {
      return TmxImportResult(created = 0, updated = 0, skipped = 0)
    }

    val existingEntries = translationMemoryEntryRepository.findByTranslationMemoryId(tm.id)

    // Index existing entries by (tuid, targetLanguageTag) for conflict resolution
    val existingByTuidAndLang =
      existingEntries
        .filter { it.tuid != null }
        .groupBy { Pair(it.tuid!!, it.targetLanguageTag) }
        .mapValues { it.value.first() }

    val createdTuids = mutableSetOf<String?>()
    val updatedTuids = mutableSetOf<String?>()
    val skippedTuids = mutableSetOf<String?>()
    val toSave = mutableListOf<TranslationMemoryEntry>()

    for (entry in parsed) {
      val tuid = entry.tuid

      if (tuid != null) {
        // Entry has a tuid — check for conflict
        val conflictKey = Pair(tuid, entry.targetLanguageTag)
        val existing = existingByTuidAndLang[conflictKey]

        if (existing != null) {
          // Conflict: same tuid + same target language
          if (overrideExisting) {
            if (existing.targetText == entry.targetText && existing.sourceText == entry.sourceText) {
              skippedTuids.add(tuid)
            } else {
              existing.sourceText = entry.sourceText
              existing.targetText = entry.targetText
              toSave.add(existing)
              updatedTuids.add(tuid)
            }
          } else {
            // Keep existing
            skippedTuids.add(tuid)
          }
        } else {
          // No conflict — create
          toSave.add(createEntry(tm, entry))
          createdTuids.add(tuid)
        }
      } else {
        // No tuid — always create
        toSave.add(createEntry(tm, entry))
        createdTuids.add(null)
      }
    }

    if (toSave.isNotEmpty()) {
      translationMemoryEntryRepository.saveAll(toSave)
    }

    // Count by distinct tuids (translation units)
    val created = createdTuids.size
    val updated = (updatedTuids - createdTuids).size
    val skipped = (skippedTuids - createdTuids - updatedTuids).size
    return TmxImportResult(created = created, updated = updated, skipped = skipped)
  }

  private fun createEntry(
    tm: TranslationMemory,
    parsed: TmxParsedEntry,
  ): TranslationMemoryEntry {
    return TranslationMemoryEntry().apply {
      this.translationMemory = tm
      this.sourceText = parsed.sourceText
      this.targetText = parsed.targetText
      this.targetLanguageTag = parsed.targetLanguageTag
      this.tuid = parsed.tuid
    }
  }
}
