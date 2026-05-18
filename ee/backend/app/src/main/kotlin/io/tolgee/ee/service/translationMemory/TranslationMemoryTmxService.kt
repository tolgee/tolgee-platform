package io.tolgee.ee.service.translationMemory

import io.tolgee.ee.data.translationMemory.TmxImportResult
import io.tolgee.ee.service.translationMemory.tmx.TmxExportUnit
import io.tolgee.ee.service.translationMemory.tmx.TmxExporter
import io.tolgee.ee.service.translationMemory.tmx.TmxParsedEntry
import io.tolgee.ee.service.translationMemory.tmx.TmxParser
import io.tolgee.model.translationMemory.TranslationMemory
import io.tolgee.model.translationMemory.TranslationMemoryEntry
import io.tolgee.repository.translationMemory.TranslationMemoryEntryRepository
import io.tolgee.repository.translationMemory.TranslationMemoryProjectRepository
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.InputStream

/**
 * TMX import + export end-to-end. Export hydrates a TM's full content (stored entries plus
 * virtual rows from write-access-assigned projects), packs it into [TmxExportUnit]s, and
 * delegates serialization to [TmxExporter]. Import parses with [TmxParser] and reconciles
 * incoming `<tu>`s against existing entries by `(tuid, lang)`.
 */
@Service
class TranslationMemoryTmxService(
  private val translationMemoryEntryRepository: TranslationMemoryEntryRepository,
  private val translationMemoryProjectRepository: TranslationMemoryProjectRepository,
  private val entityManager: EntityManager,
) {
  fun exportTmx(tm: TranslationMemory): ByteArray {
    val units = findExportUnits(tm)
    return TmxExporter(tm.sourceLanguageTag, units).export()
  }

  @Transactional
  fun importTmx(
    tm: TranslationMemory,
    inputStream: InputStream,
    overrideExisting: Boolean,
  ): TmxImportResult {
    val parseResult = TmxParser(tm.sourceLanguageTag).parse(inputStream)
    val parsed = parseResult.entries
    if (parsed.isEmpty()) {
      return TmxImportResult(created = 0, updated = 0, skipped = parseResult.skippedOversize)
    }

    val existingEntries = translationMemoryEntryRepository.findByTranslationMemoryId(tm.id)

    val existingByTuidAndLang =
      existingEntries
        .filter { it.tuid != null }
        .groupBy { Pair(it.tuid!!, it.targetLanguageTag) }
        .mapValues { it.value.first() }

    val createdTuids = mutableSetOf<String?>()
    val updatedTuids = mutableSetOf<String?>()
    val skippedTuids = mutableSetOf<String?>()
    val toSave = mutableListOf<TranslationMemoryEntry>()
    // Tracks `(tuid, lang)` pairs already accounted for during this single import — covers the
    // case where one TMX file contains the same identity multiple times. Without it the
    // existingByTuidAndLang map never sees the freshly-created entry, so the second occurrence
    // would slip into the "create new" branch and insert a duplicate.
    val seenInBatch = mutableSetOf<Pair<String, String>>()

    for (entry in parsed) {
      val tuid = entry.tuid

      if (tuid != null) {
        val conflictKey = Pair(tuid, entry.targetLanguageTag)
        if (conflictKey in seenInBatch) {
          skippedTuids.add(tuid)
          continue
        }
        seenInBatch.add(conflictKey)
        val existing = existingByTuidAndLang[conflictKey]

        if (existing != null) {
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
            skippedTuids.add(tuid)
          }
        } else {
          toSave.add(createEntry(tm, entry))
          createdTuids.add(tuid)
        }
      } else {
        toSave.add(createEntry(tm, entry))
        createdTuids.add(null)
      }
    }

    if (toSave.isNotEmpty()) {
      translationMemoryEntryRepository.saveAll(toSave)
    }

    // Count by distinct tuids — null tuid is treated as one bucket.
    // Oversize segments from the parser are folded in so the UI's existing "skipped" toast
    // surfaces them instead of silently swallowing long-text rows.
    val created = createdTuids.size
    val updated = (updatedTuids - createdTuids).size
    val skipped = (skippedTuids - createdTuids - updatedTuids).size + parseResult.skippedOversize
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

  /**
   * Hydrates a TM's full content for export — stored entries plus virtual rows computed from
   * every write-access-assigned project's translations. Virtual rows are produced as transient
   * (non-persisted) units with no `id` or `tuid`; the exporter only reads
   * sourceText/targetText/targetLanguageTag/tuid.
   */
  private fun findExportUnits(tm: TranslationMemory): List<TmxExportUnit> {
    val stored = translationMemoryEntryRepository.findByTranslationMemoryId(tm.id)
    val storedUnits = buildStoredExportUnits(stored)

    val projectIds = translationMemoryProjectRepository.findWriteAccessProjectIds(tm.id)
    if (projectIds.isEmpty()) return storedUnits

    val virtualRows = findAllVirtualRowsForProjects(projectIds, tm.writeOnlyReviewed)
    return storedUnits + buildVirtualExportUnits(virtualRows)
  }

  /**
   * Stored entries grouped by `(tuid, sourceText)`. A `tuid` group only ever maps to one
   * source in practice — pairing with sourceText keeps malformed historic data from
   * collapsing two unrelated translations into a single `<tu>`.
   */
  private fun buildStoredExportUnits(entries: List<TranslationMemoryEntry>): List<TmxExportUnit> =
    entries
      .groupBy { it.tuid to it.sourceText }
      .flatMap { (key, group) ->
        val (tuid, sourceText) = key
        val pairs = group.map { it.targetLanguageTag to it.targetText }
        splitForUniqueLangs(pairs).mapIndexed { index, langPairs ->
          // Only the first split keeps the original tuid; further splits get auto-assigned
          // numbers by the writer so siblings never share a tuid attribute.
          TmxExportUnit(
            tuid = if (index == 0) tuid else null,
            sourceText = sourceText,
            translations = langPairs,
          )
        }
      }

  /**
   * One `<tu>` per virtual row identity `(project_id, key_name)` — matches the UI's entry
   * count. Each row carries `(project_id, key_name, sourceText, targetText, targetLang)`;
   * grouping collapses multiple target languages on the same key into one `<tu>` with one
   * `<tuv>` per lang. `splitForUniqueLangs` is a safety net for the (rare) case where the
   * same project key has more than one translation for the same lang.
   */
  private fun buildVirtualExportUnits(rows: List<Array<Any?>>): List<TmxExportUnit> =
    rows
      .groupBy { (it[0] as Number).toLong() to (it[1] as String) }
      .flatMap { (_, keyRows) ->
        val sourceText = keyRows.first()[2] as String
        val pairs = keyRows.map { (it[4] as String) to (it[3] as String) }
        splitForUniqueLangs(pairs).map { langPairs ->
          TmxExportUnit(tuid = null, sourceText = sourceText, translations = langPairs)
        }
      }

  /**
   * Packs `(lang, text)` pairs into the minimum number of buckets such that each bucket has
   * at most one entry per lang. Greedy first-fit: a pair goes into the first existing bucket
   * whose langs don't already cover it; otherwise opens a new bucket.
   */
  private fun splitForUniqueLangs(pairs: List<Pair<String, String>>): List<List<Pair<String, String>>> {
    val buckets = mutableListOf<MutableList<Pair<String, String>>>()
    for (pair in pairs) {
      val target = buckets.firstOrNull { bucket -> bucket.none { it.first == pair.first } }
      if (target != null) {
        target.add(pair)
        continue
      }
      buckets.add(mutableListOf(pair))
    }
    return buckets
  }

  private fun findAllVirtualRowsForProjects(
    projectIds: List<Long>,
    writeOnlyReviewed: Boolean,
  ): List<Array<Any?>> {
    if (projectIds.isEmpty()) return emptyList()
    val sql =
      """
      select p.id as project_id,
             k.name as key_name,
             base_t.text as source_text,
             target_t.text as target_text,
             target_lang.tag as target_lang
      from project p
      join key k on k.project_id = p.id and k.deleted_at is null
      left join branch b on b.id = k.branch_id
      join translation base_t on base_t.key_id = k.id and base_t.language_id = p.base_language_id
      join translation target_t on target_t.key_id = k.id and target_t.language_id <> p.base_language_id
      join language target_lang on target_lang.id = target_t.language_id
      where p.id = any(:projectIds)
        and p.deleted_at is null
        and base_t.text is not null and base_t.text <> ''
        and target_t.text is not null and target_t.text <> ''
        and (b.id is null or b.is_default = true)
        and (not :writeOnlyReviewed or target_t.state = 2)
      order by p.id, k.name, target_lang.tag
      """.trimIndent()
    @Suppress("UNCHECKED_CAST")
    return entityManager
      .createNativeQuery(sql)
      .setParameter("projectIds", projectIds.toTypedArray())
      .setParameter("writeOnlyReviewed", writeOnlyReviewed)
      .resultList as List<Array<Any?>>
  }
}
