package io.tolgee.ee.service.translationMemory

import io.tolgee.constants.Message
import io.tolgee.ee.data.translationMemory.CreateTranslationMemoryEntryRequest
import io.tolgee.ee.data.translationMemory.UpdateTranslationMemoryEntryRequest
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.translationMemory.TranslationMemory
import io.tolgee.model.translationMemory.TranslationMemoryEntry
import io.tolgee.model.translationMemory.TranslationMemoryType
import io.tolgee.repository.translationMemory.TranslationMemoryEntryRepository
import io.tolgee.repository.translationMemory.TranslationMemoryEntrySourceRepository
import io.tolgee.repository.translationMemory.TranslationMemoryProjectRepository
import io.tolgee.repository.translationMemory.TranslationMemoryRepository
import jakarta.persistence.EntityManager
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * CRUD for individual entries inside a Translation Memory. Manual entries (`is_manual = true`)
 * are editable by the user via the add-entry dialog and TMX import; they exist on every TM type.
 * Synced entries (`is_manual = false`) are written by the translation-save pipeline and only
 * exist on SHARED TMs — they are read-only here ([update] and [delete] refuse them). Virtual
 * entries are computed on-read from the assigned project's translations and only exist on
 * PROJECT TMs; they have no row in `translation_memory_entry` and appear only in
 * [listEntryGroups] output.
 *
 * What a TM's content browser shows:
 * - SHARED → manual + synced (both are stored).
 * - PROJECT → manual + virtual (manual stored, virtual computed).
 */
@Service
class TranslationMemoryEntryManagementService(
  private val translationMemoryRepository: TranslationMemoryRepository,
  private val translationMemoryEntryRepository: TranslationMemoryEntryRepository,
  private val translationMemoryEntrySourceRepository: TranslationMemoryEntrySourceRepository,
  private val translationMemoryProjectRepository: TranslationMemoryProjectRepository,
  private val entityManager: EntityManager,
) {
  /**
   * Lists rows shown in the TM content browser. One row per `(sourceText, origin)` where
   * `origin` is one of `manual`, `synced`, `virtual`. Each row carries cells for every target
   * language matched by the optional [targetLanguageTag] filter, a flag indicating whether the
   * row is user-editable (`isManual`), and the list of contributing key names.
   *
   * SHARED TMs return manual + synced rows (both stored in `translation_memory_entry`). PROJECT
   * TMs return manual stored rows merged with virtual rows computed from the assigned project's
   * translations. Pagination spans both kinds — distinct `(sourceText, isManual)` pairs are
   * paginated at SQL level so the content stays stable under infinite scroll regardless of
   * project size.
   *
   * The language filter only narrows the entries within a row; rows still appear for source
   * texts that have no translation in the selected languages (the cells are empty). This keeps
   * the display stable as the user toggles languages.
   */
  fun listEntryGroups(
    organizationId: Long,
    translationMemoryId: Long,
    pageable: Pageable,
    search: String? = null,
    targetLanguageTag: String? = null,
  ): Page<TranslationMemoryEntryGroup> {
    val tm = requireTmInOrganization(organizationId, translationMemoryId)
    val effectiveSearch = search?.takeIf { it.isNotBlank() }
    val effectiveLang = targetLanguageTag?.takeIf { it.isNotBlank() }

    if (tm.type != TranslationMemoryType.PROJECT) {
      return loadStoredGroups(
        tmId = translationMemoryId,
        search = effectiveSearch,
        targetLanguageTag = effectiveLang,
        pageable = pageable,
      )
    }

    return loadProjectGroupsPaged(
      tm = tm,
      projectId = projectIdForTm(tm),
      search = effectiveSearch,
      targetLanguageTag = effectiveLang,
      pageable = pageable,
    )
  }

  private fun projectIdForTm(tm: TranslationMemory): Long? {
    if (tm.type != TranslationMemoryType.PROJECT) return null
    return translationMemoryProjectRepository
      .findByTranslationMemoryId(tm.id)
      .firstOrNull()
      ?.project
      ?.id
  }

  private fun loadStoredGroups(
    tmId: Long,
    search: String?,
    targetLanguageTag: String?,
    pageable: Pageable,
  ): Page<TranslationMemoryEntryGroup> {
    val groupKeysPage =
      translationMemoryEntryRepository.findDistinctGroupKeysPaged(
        translationMemoryId = tmId,
        search = search,
        pageable = pageable,
      )
    if (groupKeysPage.content.isEmpty()) {
      return PageImpl(emptyList(), pageable, groupKeysPage.totalElements)
    }

    val distinctSourceTexts = groupKeysPage.content.map { it[0] as String }.distinct()
    val entries =
      translationMemoryEntryRepository.findByTranslationMemoryIdAndSourceTexts(
        translationMemoryId = tmId,
        sourceTexts = distinctSourceTexts.toTypedArray(),
        targetLanguageTags = targetLanguageTag,
      )
    val entryBuckets = entries.groupBy { StoredGroupKey(it.sourceText, it.isManual) }

    // Batch-lookup key names per entry so the content-browser renders provenance chips without
    // N+1 queries per row.
    val allEntryIds = entries.map { it.id }
    val keyNamesByEntry = mutableMapOf<Long, MutableList<String>>()
    if (allEntryIds.isNotEmpty()) {
      translationMemoryEntrySourceRepository
        .findKeyNamesByEntryIds(allEntryIds)
        .forEach { row ->
          val entryId = (row[0] as Number).toLong()
          val keyName = row[1] as String
          keyNamesByEntry.getOrPut(entryId) { mutableListOf() }.add(keyName)
        }
    }

    val groups =
      groupKeysPage.content.map { row ->
        val sourceText = row[0] as String
        val isManual = row[1] as Boolean
        val bucketed = entryBuckets[StoredGroupKey(sourceText, isManual)].orEmpty()
        val keyNames =
          if (isManual) {
            emptyList()
          } else {
            bucketed.flatMap { keyNamesByEntry[it.id].orEmpty() }.distinct().sorted()
          }
        TranslationMemoryEntryGroup(
          sourceText = sourceText,
          keyNames = keyNames,
          isManual = isManual,
          entries = bucketed,
          virtualEntries = emptyList(),
        )
      }
    return PageImpl(groups, pageable, groupKeysPage.totalElements)
  }

  /**
   * Paginates a PROJECT TM's content at the SQL level. The page boundary is the union of
   *  - distinct manual `(sourceText, true)` keys from `translation_memory_entry`, and
   *  - distinct virtual `(sourceText, false)` keys derived from the assigned project's translations.
   *
   * For each page key we hydrate either the stored row (manual) or the per-language virtual rows
   * (synthesized from the project translations). This keeps the listing stable under infinite
   * scroll regardless of project size — the earlier implementation pulled every virtual row into
   * memory on each request, which produced an "infinite" list for projects with many translations.
   *
   * When the project TM has no assigned project (rare, only possible if the project was deleted
   * out from under the TM), virtual content is empty and only manual entries surface.
   */
  private fun loadProjectGroupsPaged(
    tm: TranslationMemory,
    projectId: Long?,
    search: String?,
    targetLanguageTag: String?,
    pageable: Pageable,
  ): Page<TranslationMemoryEntryGroup> {
    // Sentinel keeps the union SQL valid when the TM has no assigned project; postgres rejects
    // bound NULLs in `p.id = :projectId`, but `p.id = -1` simply yields zero virtual rows so only
    // manual entries surface — which is the right fallback.
    val safeProjectId = projectId ?: -1L
    val total = countProjectGroupKeys(tm.id, safeProjectId, tm.writeOnlyReviewed, search)
    if (total == 0L) {
      return PageImpl(emptyList(), pageable, 0)
    }

    val pageKeys =
      findProjectGroupKeysPaged(
        tmId = tm.id,
        projectId = safeProjectId,
        writeOnlyReviewed = tm.writeOnlyReviewed,
        search = search,
        pageable = pageable,
      )
    if (pageKeys.isEmpty()) {
      return PageImpl(emptyList(), pageable, total)
    }

    val manualSourceTexts = pageKeys.filter { it.isManual }.map { it.sourceText }.distinct()
    val virtualSourceTexts = pageKeys.filter { !it.isManual }.map { it.sourceText }.distinct()

    val manualEntriesBySourceText = hydrateManualEntries(tm.id, manualSourceTexts, targetLanguageTag)
    val virtualRowsBySourceText =
      hydrateVirtualRows(projectId, tm.writeOnlyReviewed, virtualSourceTexts, targetLanguageTag)

    val groups =
      pageKeys.map { key ->
        buildProjectGroup(key, manualEntriesBySourceText, virtualRowsBySourceText)
      }
    return PageImpl(groups, pageable, total)
  }

  private fun hydrateManualEntries(
    tmId: Long,
    manualSourceTexts: List<String>,
    targetLanguageTag: String?,
  ): Map<String, List<TranslationMemoryEntry>> {
    if (manualSourceTexts.isEmpty()) return emptyMap()
    return translationMemoryEntryRepository
      .findByTranslationMemoryIdAndSourceTexts(
        translationMemoryId = tmId,
        sourceTexts = manualSourceTexts.toTypedArray(),
        targetLanguageTags = targetLanguageTag,
      ).filter { it.isManual }
      .groupBy { it.sourceText }
  }

  private fun hydrateVirtualRows(
    projectId: Long?,
    writeOnlyReviewed: Boolean,
    virtualSourceTexts: List<String>,
    targetLanguageTag: String?,
  ): Map<String, List<VirtualSourceRow>> {
    if (projectId == null || virtualSourceTexts.isEmpty()) return emptyMap()
    val rows =
      findVirtualRowsForSourceTexts(
        projectId = projectId,
        writeOnlyReviewed = writeOnlyReviewed,
        sourceTexts = virtualSourceTexts,
        targetLanguageTag = targetLanguageTag,
      )
    val bySource = mutableMapOf<String, MutableList<VirtualSourceRow>>()
    for (row in rows) {
      val sourceText = row[0] as String
      bySource
        .getOrPut(sourceText) { mutableListOf() }
        .add(
          VirtualSourceRow(
            targetText = row[1] as String,
            targetLang = row[2] as String,
            keyName = row[3] as String,
          ),
        )
    }
    return bySource
  }

  private fun buildProjectGroup(
    key: ProjectGroupKey,
    manualEntriesBySourceText: Map<String, List<TranslationMemoryEntry>>,
    virtualRowsBySourceText: Map<String, List<VirtualSourceRow>>,
  ): TranslationMemoryEntryGroup {
    if (key.isManual) {
      return TranslationMemoryEntryGroup(
        sourceText = key.sourceText,
        keyNames = emptyList(),
        isManual = true,
        entries = manualEntriesBySourceText[key.sourceText].orEmpty(),
        virtualEntries = emptyList(),
      )
    }
    val srcRows = virtualRowsBySourceText[key.sourceText].orEmpty()
    val virtualEntries =
      srcRows
        .map {
          VirtualEntry(
            sourceText = key.sourceText,
            targetText = it.targetText,
            targetLanguageTag = it.targetLang,
          )
        }.distinct()
    return TranslationMemoryEntryGroup(
      sourceText = key.sourceText,
      keyNames = srcRows.map { it.keyName }.distinct().sorted(),
      isManual = false,
      entries = emptyList(),
      virtualEntries = virtualEntries,
    )
  }

  /**
   * Counts distinct `(source_text, is_manual)` pairs across stored manual entries on this TM and
   * virtual rows derived from the assigned project. The count drives the total used by the page
   * model so the front-end's infinite scroll knows when to stop.
   */
  private fun countProjectGroupKeys(
    tmId: Long,
    projectId: Long,
    writeOnlyReviewed: Boolean,
    search: String?,
  ): Long {
    val sql =
      """
      select count(*) from (
        ${projectGroupKeysUnionSql()}
      ) sub
      """.trimIndent()
    val result =
      entityManager
        .createNativeQuery(sql)
        .setParameter("tmId", tmId)
        .setParameter("projectId", projectId)
        .setParameter("writeOnlyReviewed", writeOnlyReviewed)
        .setParameter("search", search)
        .singleResult
    return (result as Number).toLong()
  }

  /**
   * Returns one page of distinct `(source_text, is_manual)` keys ordered so manual rows come
   * before virtual rows within the same source text — matches the SHARED-TM listing's ordering.
   */
  private fun findProjectGroupKeysPaged(
    tmId: Long,
    projectId: Long,
    writeOnlyReviewed: Boolean,
    search: String?,
    pageable: Pageable,
  ): List<ProjectGroupKey> {
    val sql =
      """
      select source_text, is_manual from (
        ${projectGroupKeysUnionSql()}
      ) sub
      order by source_text, is_manual desc
      limit :limit offset :offset
      """.trimIndent()

    @Suppress("UNCHECKED_CAST")
    val rows =
      entityManager
        .createNativeQuery(sql)
        .setParameter("tmId", tmId)
        .setParameter("projectId", projectId)
        .setParameter("writeOnlyReviewed", writeOnlyReviewed)
        .setParameter("search", search)
        .setParameter("limit", pageable.pageSize)
        .setParameter("offset", pageable.offset)
        .resultList as List<Array<Any?>>
    return rows.map { ProjectGroupKey(sourceText = it[0] as String, isManual = it[1] as Boolean) }
  }

  /**
   * SQL fragment that produces `(source_text, is_manual)` rows for a PROJECT TM:
   * - manual half: distinct sources of stored manual entries on this TM
   * - virtual half: distinct sources of writable target translations on the assigned project
   *
   * Both halves apply the same case-insensitive search filter against either source or target
   * text. UNION (without ALL) deduplicates within each half so the outer pagination sees a
   * stable, ordered key list.
   */
  private fun projectGroupKeysUnionSql(): String =
    """
    select e.source_text as source_text, true as is_manual
    from translation_memory_entry e
    where e.translation_memory_id = :tmId
      and e.is_manual = true
      and (
        cast(:search as text) is null
        or lower(e.source_text::text) like lower('%' || cast(:search as text) || '%')
        or exists (
          select 1 from translation_memory_entry e2
          where e2.translation_memory_id = e.translation_memory_id
            and e2.source_text = e.source_text
            and e2.is_manual = true
            and lower(e2.target_text::text) like lower('%' || cast(:search as text) || '%')
        )
      )
    group by e.source_text

    union

    select base_t.text as source_text, false as is_manual
    from project p
    join language base_lang on base_lang.id = p.base_language_id
    join key k on k.project_id = p.id
    left join branch b on b.id = k.branch_id
    join translation base_t on base_t.key_id = k.id and base_t.language_id = base_lang.id
    join translation target_t on target_t.key_id = k.id and target_t.language_id <> base_lang.id
    where p.id = :projectId
      and base_t.text is not null and base_t.text <> ''
      and target_t.text is not null and target_t.text <> ''
      and (b.id is null or b.is_default = true)
      and (not :writeOnlyReviewed or target_t.state = 2)
      and (
        cast(:search as text) is null
        or lower(base_t.text) like lower('%' || cast(:search as text) || '%')
        or lower(target_t.text) like lower('%' || cast(:search as text) || '%')
      )
    group by base_t.text
    """.trimIndent()

  /**
   * Hydrates a PROJECT TM's full content for TMX export — manual stored entries plus virtual rows
   * computed from the assigned project's translations. The export must mirror what the content
   * browser shows; otherwise the round-trip "see in UI → export → re-import elsewhere" loses
   * data.
   *
   * Virtual rows are returned as transient (non-persisted) [TranslationMemoryEntry] objects so
   * the caller can feed them to [io.tolgee.ee.service.translationMemory.tmx.TmxExporter] alongside
   * stored ones — the exporter only reads `sourceText`, `targetText`, `targetLanguageTag`, and
   * `tuid` (null on virtual rows since they have no stable identity to round-trip). Returns an
   * empty list for non-PROJECT TMs.
   */
  fun findEntriesForProjectTmExport(tm: TranslationMemory): List<TranslationMemoryEntry> {
    if (tm.type != TranslationMemoryType.PROJECT) return emptyList()
    val manual =
      translationMemoryEntryRepository
        .findByTranslationMemoryId(tm.id)
        .filter { it.isManual }
    val projectId = projectIdForTm(tm) ?: return manual
    val virtualRows = findAllDistinctVirtualRowsForProject(projectId, tm.writeOnlyReviewed)
    val virtual =
      virtualRows.map { row ->
        TranslationMemoryEntry().apply {
          this.translationMemory = tm
          this.sourceText = row[0] as String
          this.targetText = row[1] as String
          this.targetLanguageTag = row[2] as String
        }
      }
    return manual + virtual
  }

  private fun findAllDistinctVirtualRowsForProject(
    projectId: Long,
    writeOnlyReviewed: Boolean,
  ): List<Array<Any?>> {
    val sql =
      """
      select distinct base_t.text as source_text,
             target_t.text as target_text,
             target_lang.tag as target_lang
      from project p
      join language base_lang on base_lang.id = p.base_language_id
      join key k on k.project_id = p.id
      left join branch b on b.id = k.branch_id
      join translation base_t on base_t.key_id = k.id and base_t.language_id = base_lang.id
      join translation target_t on target_t.key_id = k.id and target_t.language_id <> base_lang.id
      join language target_lang on target_lang.id = target_t.language_id
      where p.id = :projectId
        and base_t.text is not null and base_t.text <> ''
        and target_t.text is not null and target_t.text <> ''
        and (b.id is null or b.is_default = true)
        and (not :writeOnlyReviewed or target_t.state = 2)
      order by base_t.text, target_lang.tag
      """.trimIndent()
    @Suppress("UNCHECKED_CAST")
    return entityManager
      .createNativeQuery(sql)
      .setParameter("projectId", projectId)
      .setParameter("writeOnlyReviewed", writeOnlyReviewed)
      .resultList as List<Array<Any?>>
  }

  private fun findVirtualRowsForSourceTexts(
    projectId: Long,
    writeOnlyReviewed: Boolean,
    sourceTexts: List<String>,
    targetLanguageTag: String?,
  ): List<Array<Any?>> {
    val sql =
      """
      select base_t.text as source_text,
             target_t.text as target_text,
             target_lang.tag as target_lang,
             k.name as key_name
      from project p
      join language base_lang on base_lang.id = p.base_language_id
      join key k on k.project_id = p.id
      left join branch b on b.id = k.branch_id
      join translation base_t on base_t.key_id = k.id and base_t.language_id = base_lang.id
      join translation target_t on target_t.key_id = k.id and target_t.language_id <> base_lang.id
      join language target_lang on target_lang.id = target_t.language_id
      where p.id = :projectId
        and base_t.text = any(:sourceTexts)
        and target_t.text is not null and target_t.text <> ''
        and (b.id is null or b.is_default = true)
        and (not :writeOnlyReviewed or target_t.state = 2)
        and (
          cast(:targetLanguageTags as text) is null
          or target_lang.tag = any(string_to_array(cast(:targetLanguageTags as text), ','))
        )
      order by base_t.text, target_lang.tag
      """.trimIndent()
    @Suppress("UNCHECKED_CAST")
    return entityManager
      .createNativeQuery(sql)
      .setParameter("projectId", projectId)
      .setParameter("writeOnlyReviewed", writeOnlyReviewed)
      .setParameter("sourceTexts", sourceTexts.toTypedArray())
      .setParameter("targetLanguageTags", targetLanguageTag)
      .resultList as List<Array<Any?>>
  }

  private data class StoredGroupKey(
    val sourceText: String,
    val isManual: Boolean,
  )

  private data class ProjectGroupKey(
    val sourceText: String,
    val isManual: Boolean,
  )

  private data class VirtualSourceRow(
    val targetText: String,
    val targetLang: String,
    val keyName: String,
  )

  fun getEntry(
    organizationId: Long,
    translationMemoryId: Long,
    entryId: Long,
  ): TranslationMemoryEntry {
    requireTmInOrganization(organizationId, translationMemoryId)
    val entry =
      translationMemoryEntryRepository.findById(entryId).orElseThrow {
        NotFoundException(Message.TRANSLATION_MEMORY_ENTRY_NOT_FOUND)
      }
    if (entry.translationMemory.id != translationMemoryId) {
      throw NotFoundException(Message.TRANSLATION_MEMORY_ENTRY_NOT_FOUND)
    }
    return entry
  }

  @Transactional
  fun create(
    organizationId: Long,
    translationMemoryId: Long,
    dto: CreateTranslationMemoryEntryRequest,
  ): TranslationMemoryEntry {
    val tm = requireTmInOrganization(organizationId, translationMemoryId)
    val entry =
      TranslationMemoryEntry().apply {
        this.translationMemory = tm
        this.sourceText = dto.sourceText
        this.targetText = dto.targetText
        this.targetLanguageTag = dto.targetLanguageTag
        this.isManual = true
      }
    return translationMemoryEntryRepository.save(entry)
  }

  @Transactional
  fun update(
    organizationId: Long,
    translationMemoryId: Long,
    entryId: Long,
    dto: UpdateTranslationMemoryEntryRequest,
  ): TranslationMemoryEntry {
    val entry = getEntry(organizationId, translationMemoryId, entryId)
    if (!entry.isManual) {
      // Synced entries track project translations — editing them here would clash with the next
      // `onTranslationSaved` pass. Force the user to edit the source translation instead.
      throw BadRequestException(Message.TRANSLATION_MEMORY_ENTRY_READ_ONLY)
    }
    entry.sourceText = dto.sourceText
    entry.targetText = dto.targetText
    entry.targetLanguageTag = dto.targetLanguageTag
    return translationMemoryEntryRepository.save(entry)
  }

  @Transactional
  fun delete(
    organizationId: Long,
    translationMemoryId: Long,
    entryId: Long,
  ) {
    val entry = getEntry(organizationId, translationMemoryId, entryId)
    translationMemoryEntryRepository.delete(entry)
  }

  /**
   * Deletes every entry in the TM whose source text matches the group the passed [entryId]
   * belongs to — i.e. the whole row visible in the content browser. Both synced and manual
   * entries can be deleted this way; synced rows re-materialize on the next `onTranslationSaved`
   * pass for any remaining linked translation.
   */
  @Transactional
  fun deleteGroup(
    organizationId: Long,
    translationMemoryId: Long,
    entryId: Long,
  ): Int {
    val entry = getEntry(organizationId, translationMemoryId, entryId)
    return translationMemoryEntryRepository.deleteByTranslationMemoryIdAndSourceTextAndIsManual(
      translationMemoryId = translationMemoryId,
      sourceText = entry.sourceText,
      isManual = entry.isManual,
    )
  }

  /**
   * Batch variant of [deleteGroup]. For every entry ID in the payload, wipes the entire row
   * (shared `sourceText + isManual` bucket) that entry belongs to. Dedupes to distinct rows so
   * passing multiple entries from the same row only runs the delete once.
   */
  @Transactional
  fun deleteMultipleGroups(
    organizationId: Long,
    translationMemoryId: Long,
    entryIds: Set<Long>,
  ): Int {
    requireTmInOrganization(organizationId, translationMemoryId)
    if (entryIds.isEmpty()) return 0
    val entries =
      translationMemoryEntryRepository
        .findAllById(entryIds)
        .filter { it.translationMemory.id == translationMemoryId }
    if (entries.isEmpty()) return 0
    val groupKeys = entries.map { StoredGroupKey(it.sourceText, it.isManual) }.toSet()
    var totalDeleted = 0
    for (key in groupKeys) {
      totalDeleted +=
        translationMemoryEntryRepository.deleteByTranslationMemoryIdAndSourceTextAndIsManual(
          translationMemoryId = translationMemoryId,
          sourceText = key.sourceText,
          isManual = key.isManual,
        )
    }
    return totalDeleted
  }

  private fun requireTmInOrganization(
    organizationId: Long,
    translationMemoryId: Long,
  ): TranslationMemory {
    return translationMemoryRepository.find(organizationId, translationMemoryId)
      ?: throw NotFoundException(Message.TRANSLATION_MEMORY_NOT_FOUND)
  }
}

/**
 * A virtual cell in a project TM's content browser — computed from a project translation rather
 * than stored. Structurally identical to a `(sourceText, targetText, targetLanguageTag)` triple
 * that a stored entry would have, but carries no `id` because nothing is persisted.
 */
data class VirtualEntry(
  val sourceText: String,
  val targetText: String,
  val targetLanguageTag: String,
)
