package io.tolgee.ee.service.translationMemory

import io.tolgee.constants.Message
import io.tolgee.ee.data.translationMemory.CreateMultipleTranslationMemoryEntriesRequest
import io.tolgee.ee.data.translationMemory.CreateTranslationMemoryEntryRequest
import io.tolgee.ee.data.translationMemory.UpdateTranslationMemoryEntryRequest
import io.tolgee.ee.service.translationMemory.tmx.TmxExportUnit
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.translationMemory.TranslationMemory
import io.tolgee.model.translationMemory.TranslationMemoryEntry
import io.tolgee.repository.translationMemory.TranslationMemoryEntryRepository
import io.tolgee.repository.translationMemory.TranslationMemoryProjectRepository
import io.tolgee.repository.translationMemory.TranslationMemoryRepository
import jakarta.persistence.EntityManager
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * CRUD for individual entries inside a Translation Memory.
 *
 * A TM's content = stored entries (created via the add-entry dialog or TMX import) plus
 * virtual rows computed on read from every project assigned with `writeAccess=true`. This
 * applies uniformly to every TM — PROJECT and SHARED types differ only in assignment
 * lifecycle, not in content composition.
 */
@Service
class TranslationMemoryEntryManagementService(
  private val translationMemoryRepository: TranslationMemoryRepository,
  private val translationMemoryEntryRepository: TranslationMemoryEntryRepository,
  private val translationMemoryProjectRepository: TranslationMemoryProjectRepository,
  private val entityManager: EntityManager,
) {
  /**
   * Lists rows shown in the TM content browser. A row is either stored (one per
   * `(sourceText, tuid-or-null)` bucket — manual entries on a source collapse into one
   * "manual" bucket; each TMX tuid is its own bucket) or virtual (one per
   * `(sourceText, project, key)`). Pagination is row-level so a single source text can
   * fan out into N rows in the page when multiple origins share it.
   *
   * The optional [targetLanguageTag] narrows the *cells* of each row to a subset of target
   * languages; rows themselves still appear for every origin matching `:search`.
   */
  fun listEntryRows(
    organizationId: Long,
    translationMemoryId: Long,
    pageable: Pageable,
    search: String? = null,
    targetLanguageTag: String? = null,
  ): Page<TmRow> {
    val tm = requireTmInOrganization(organizationId, translationMemoryId)
    val effectiveSearch = search?.takeIf { it.isNotBlank() }
    val effectiveLang = targetLanguageTag?.takeIf { it.isNotBlank() }
    val writeProjectIds = writeAccessProjectIds(tm.id)

    val total = countRows(tm.id, writeProjectIds, tm.writeOnlyReviewed, effectiveSearch)
    if (total == 0L) return PageImpl(emptyList(), pageable, 0)

    val rowIds =
      findRowIdsPaged(
        tmId = tm.id,
        projectIds = writeProjectIds,
        writeOnlyReviewed = tm.writeOnlyReviewed,
        search = effectiveSearch,
        pageable = pageable,
      )
    if (rowIds.isEmpty()) return PageImpl(emptyList(), pageable, total)

    val storedRowIds = rowIds.filter { it.kind == TmRow.Kind.STORED }
    val virtualRowIds = rowIds.filter { it.kind == TmRow.Kind.VIRTUAL }

    val storedByBucket = hydrateStoredRows(tm.id, storedRowIds, effectiveLang)
    val virtualByOrigin = hydrateVirtualRowsByOrigin(virtualRowIds, tm.writeOnlyReviewed, effectiveLang)

    val rows =
      rowIds.map { id ->
        when (id.kind) {
          TmRow.Kind.STORED ->
            TmRow(
              sourceText = id.sourceText,
              kind = TmRow.Kind.STORED,
              originId = id.originId,
              entries = storedByBucket[id].orEmpty(),
              virtualEntries = emptyList(),
              keyName = null,
              projectId = null,
              projectName = null,
            )
          TmRow.Kind.VIRTUAL -> {
            val origin = virtualByOrigin[id]
            TmRow(
              sourceText = id.sourceText,
              kind = TmRow.Kind.VIRTUAL,
              originId = id.originId,
              entries = emptyList(),
              virtualEntries = origin?.cells.orEmpty(),
              keyName = origin?.keyName,
              projectId = origin?.projectId,
              projectName = origin?.projectName,
            )
          }
        }
      }
    return PageImpl(rows, pageable, total)
  }

  /**
   * Returns one entry ID per STORED row matching [search] (a representative — `min(id)`
   * within each `(sourceText, tuid-or-"manual")` bucket). Virtual rows have no entry IDs
   * so they are excluded. Used by the UI to drive "select all" for batch delete without
   * paging through the whole content list.
   */
  fun findAllStoredEntryIds(
    organizationId: Long,
    translationMemoryId: Long,
    search: String? = null,
  ): List<Long> {
    val tm = requireTmInOrganization(organizationId, translationMemoryId)
    val effectiveSearch = search?.takeIf { it.isNotBlank() }

    val sql =
      """
      select min(e.id) from translation_memory_entry e
      where e.translation_memory_id = :tmId
        and (
          cast(:search as text) is null
          or lower(e.source_text::text) like lower('%' || cast(:search as text) || '%')
          or exists (
            select 1 from translation_memory_entry e2
            where e2.translation_memory_id = e.translation_memory_id
              and e2.source_text = e.source_text
              and e2.tuid is not distinct from e.tuid
              and lower(e2.target_text::text) like lower('%' || cast(:search as text) || '%')
          )
        )
      group by e.source_text, coalesce(e.tuid, 'manual')
      """.trimIndent()

    @Suppress("UNCHECKED_CAST")
    val ids =
      entityManager
        .createNativeQuery(sql)
        .setParameter("tmId", tm.id)
        .setParameter("search", effectiveSearch)
        .resultList as List<Number>
    return ids.map { it.toLong() }
  }

  private data class RowId(
    val sourceText: String,
    val kind: TmRow.Kind,
    val originId: String,
  )

  private data class VirtualOriginCells(
    val projectId: Long,
    val projectName: String,
    val keyName: String,
    val cells: List<VirtualEntry>,
  )

  private fun writeAccessProjectIds(tmId: Long): List<Long> =
    translationMemoryProjectRepository
      .findByTranslationMemoryId(tmId)
      .filter { it.writeAccess }
      .map { it.project.id }

  private fun countRows(
    tmId: Long,
    projectIds: List<Long>,
    writeOnlyReviewed: Boolean,
    search: String?,
  ): Long {
    val sql = "select count(*) from (${rowIdentitiesSql()}) sub"
    return (
      entityManager
        .createNativeQuery(sql)
        .setParameter("tmId", tmId)
        .setParameter("projectIds", projectIds.toTypedArray())
        .setParameter("writeOnlyReviewed", writeOnlyReviewed)
        .setParameter("search", search)
        .singleResult as Number
    ).toLong()
  }

  private fun findRowIdsPaged(
    tmId: Long,
    projectIds: List<Long>,
    writeOnlyReviewed: Boolean,
    search: String?,
    pageable: Pageable,
  ): List<RowId> {
    val sql =
      """
      select source_text, kind, origin_id from (${rowIdentitiesSql()}) sub
      order by source_text, kind, origin_id
      limit :limit offset :offset
      """.trimIndent()

    @Suppress("UNCHECKED_CAST")
    val rows =
      entityManager
        .createNativeQuery(sql)
        .setParameter("tmId", tmId)
        .setParameter("projectIds", projectIds.toTypedArray())
        .setParameter("writeOnlyReviewed", writeOnlyReviewed)
        .setParameter("search", search)
        .setParameter("limit", pageable.pageSize)
        .setParameter("offset", pageable.offset)
        .resultList as List<Array<Any?>>
    return rows.map {
      RowId(
        sourceText = it[0] as String,
        kind = if (it[1] == "stored") TmRow.Kind.STORED else TmRow.Kind.VIRTUAL,
        originId = it[2] as String,
      )
    }
  }

  /**
   * SQL fragment producing distinct row identities `(source_text, kind, origin_id)` from:
   *  - stored entries on this TM, grouped by `(source_text, COALESCE(tuid, 'manual'))` —
   *    each TMX tuid becomes one row; all null-tuid (manual) entries on a source collapse
   *    into a single "manual" bucket.
   *  - virtual translations on every write-access-assigned project, grouped by
   *    `(source_text, project_id, key_name)` — one row per project key.
   *
   * The search filter matches source text or any target text inside the bucket/origin, so
   * a row appears for every origin where any of its cells contains the term.
   */
  private fun rowIdentitiesSql(): String =
    """
    select
      e.source_text as source_text,
      'stored' as kind,
      coalesce(e.tuid, 'manual') as origin_id
    from translation_memory_entry e
    where e.translation_memory_id = :tmId
      and (
        cast(:search as text) is null
        or lower(e.source_text::text) like lower('%' || cast(:search as text) || '%')
        or exists (
          select 1 from translation_memory_entry e2
          where e2.translation_memory_id = e.translation_memory_id
            and e2.source_text = e.source_text
            and e2.tuid is not distinct from e.tuid
            and lower(e2.target_text::text) like lower('%' || cast(:search as text) || '%')
        )
      )
    group by e.source_text, coalesce(e.tuid, 'manual')

    union all

    select distinct
      base_t.text as source_text,
      'virtual' as kind,
      p.id::text || ':' || k.name as origin_id
    from project p
    join key k on k.project_id = p.id and k.deleted_at is null
    left join branch b on b.id = k.branch_id
    -- Reference p.base_language_id directly so PG can pick the (key_id, language_id)
    -- unique index for base_t and target_t — joining `language` separately makes the
    -- planner choose the key_id-only index and filter language at the join level.
    join translation base_t on base_t.key_id = k.id and base_t.language_id = p.base_language_id
    where p.id = any(:projectIds)
      and p.deleted_at is null
      and base_t.text is not null and base_t.text <> ''
      and (b.id is null or b.is_default = true)
      and exists (
        select 1 from translation target_t
        where target_t.key_id = k.id
          and target_t.language_id <> p.base_language_id
          and target_t.text is not null and target_t.text <> ''
          and (not :writeOnlyReviewed or target_t.state = 2)
          and (
            cast(:search as text) is null
            or lower(base_t.text) like lower('%' || cast(:search as text) || '%')
            or lower(target_t.text) like lower('%' || cast(:search as text) || '%')
          )
      )
    """.trimIndent()

  /**
   * Fetches all stored entries for the given row identities and groups them back into
   * buckets matching the (sourceText, tuid-or-"manual") origin used during pagination.
   */
  private fun hydrateStoredRows(
    tmId: Long,
    rowIds: List<RowId>,
    targetLanguageTag: String?,
  ): Map<RowId, List<TranslationMemoryEntry>> {
    if (rowIds.isEmpty()) return emptyMap()
    val sourceTexts = rowIds.map { it.sourceText }.distinct()
    val entries =
      translationMemoryEntryRepository.findByTranslationMemoryIdAndSourceTexts(
        translationMemoryId = tmId,
        sourceTexts = sourceTexts.toTypedArray(),
        targetLanguageTags = targetLanguageTag,
      )
    val rowIdSet = rowIds.toSet()
    return entries
      .groupBy {
        RowId(
          sourceText = it.sourceText,
          kind = TmRow.Kind.STORED,
          originId = it.tuid ?: "manual",
        )
      }.filterKeys { it in rowIdSet }
  }

  /**
   * Fetches virtual cells for each (projectId, keyName) origin in the page and groups them
   * back into rows.
   */
  private fun hydrateVirtualRowsByOrigin(
    rowIds: List<RowId>,
    writeOnlyReviewed: Boolean,
    targetLanguageTag: String?,
  ): Map<RowId, VirtualOriginCells> {
    if (rowIds.isEmpty()) return emptyMap()
    // origin_id = "<projectId>:<keyName>"; split back into a (projectId, keyName) pair.
    val origins =
      rowIds.mapNotNull { id ->
        val sep = id.originId.indexOf(':')
        if (sep < 0) return@mapNotNull null
        val projectId = id.originId.substring(0, sep).toLongOrNull() ?: return@mapNotNull null
        val keyName = id.originId.substring(sep + 1)
        Triple(projectId, keyName, id)
      }
    if (origins.isEmpty()) return emptyMap()
    val projectIds = origins.map { it.first }.distinct().toTypedArray()
    val keyNames = origins.map { it.second }.distinct().toTypedArray()
    val sql =
      """
      select base_t.text as source_text,
             target_t.text as target_text,
             target_lang.tag as target_lang,
             k.name as key_name,
             p.id as project_id,
             p.name as project_name
      from project p
      join language base_lang on base_lang.id = p.base_language_id
      join key k on k.project_id = p.id and k.deleted_at is null
      left join branch b on b.id = k.branch_id
      join translation base_t on base_t.key_id = k.id and base_t.language_id = base_lang.id
      join translation target_t on target_t.key_id = k.id and target_t.language_id <> base_lang.id
      join language target_lang on target_lang.id = target_t.language_id
      where p.id = any(:projectIds)
        and k.name = any(:keyNames)
        and p.deleted_at is null
        and base_t.text is not null and base_t.text <> ''
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
    val rows =
      entityManager
        .createNativeQuery(sql)
        .setParameter("projectIds", projectIds)
        .setParameter("keyNames", keyNames)
        .setParameter("writeOnlyReviewed", writeOnlyReviewed)
        .setParameter("targetLanguageTags", targetLanguageTag)
        .resultList as List<Array<Any?>>

    val cellsByOrigin = mutableMapOf<RowId, MutableList<VirtualEntry>>()
    val originMeta = mutableMapOf<RowId, Pair<Long, String>>()
    val rowIdSet = rowIds.toSet()
    for (row in rows) {
      val sourceText = row[0] as String
      val projectId = (row[4] as Number).toLong()
      val keyName = row[3] as String
      val rowId =
        RowId(
          sourceText = sourceText,
          kind = TmRow.Kind.VIRTUAL,
          originId = "$projectId:$keyName",
        )
      if (rowId !in rowIdSet) continue
      cellsByOrigin
        .getOrPut(rowId) { mutableListOf() }
        .add(
          VirtualEntry(
            sourceText = sourceText,
            targetText = row[1] as String,
            targetLanguageTag = row[2] as String,
            projectId = projectId,
            projectName = row[5] as String,
            keyName = keyName,
          ),
        )
      originMeta[rowId] = projectId to (row[5] as String)
    }
    return cellsByOrigin.mapValues { (id, cells) ->
      val (pid, pname) = originMeta[id]!!
      VirtualOriginCells(
        projectId = pid,
        projectName = pname,
        keyName = cells.first().keyName,
        cells = cells.distinct(),
      )
    }
  }

  /**
   * Hydrates a TM's full content for TMX export — stored entries plus virtual rows computed
   * from every write-access-assigned project's translations. Virtual rows are returned as
   * transient (non-persisted) entries with no `id` or `tuid`; the exporter only reads
   * sourceText/targetText/targetLanguageTag/tuid.
   */
  fun findExportUnits(tm: TranslationMemory): List<TmxExportUnit> {
    val stored = translationMemoryEntryRepository.findByTranslationMemoryId(tm.id)
    val storedUnits = buildStoredExportUnits(stored)

    val projectIds = writeAccessProjectIds(tm.id)
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
      }
    return translationMemoryEntryRepository.save(entry)
  }

  /**
   * Creates one entry per requested target language, all in one transaction. Replaces the
   * UI's previous per-language POST loop — which could leave the TM in a partially-saved state
   * if a later language failed.
   */
  @Transactional
  fun createMultiple(
    organizationId: Long,
    translationMemoryId: Long,
    dto: CreateMultipleTranslationMemoryEntriesRequest,
  ): List<TranslationMemoryEntry> {
    val tm = requireTmInOrganization(organizationId, translationMemoryId)
    val duplicateLangs =
      dto.translations
        .groupingBy { it.targetLanguageTag }
        .eachCount()
        .filterValues { it > 1 }
        .keys
    if (duplicateLangs.isNotEmpty()) {
      throw BadRequestException(
        Message.TRANSLATION_MEMORY_ENTRY_DUPLICATE_TARGET_LANGUAGE,
        listOf(duplicateLangs.sorted().joinToString(", ")),
      )
    }
    val entries =
      dto.translations.map { translation ->
        TranslationMemoryEntry().apply {
          this.translationMemory = tm
          this.sourceText = dto.sourceText
          this.targetText = translation.targetText
          this.targetLanguageTag = translation.targetLanguageTag
        }
      }
    return translationMemoryEntryRepository.saveAll(entries)
  }

  @Transactional
  fun update(
    organizationId: Long,
    translationMemoryId: Long,
    entryId: Long,
    dto: UpdateTranslationMemoryEntryRequest,
  ): TranslationMemoryEntry {
    val entry = getEntry(organizationId, translationMemoryId, entryId)
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

  /** Deletes every stored entry sharing the source text of [entryId]. */
  @Transactional
  fun deleteGroup(
    organizationId: Long,
    translationMemoryId: Long,
    entryId: Long,
  ): Int {
    val entry = getEntry(organizationId, translationMemoryId, entryId)
    return translationMemoryEntryRepository.deleteByTranslationMemoryIdAndSourceText(
      translationMemoryId = translationMemoryId,
      sourceText = entry.sourceText,
    )
  }

  /** Batch variant of [deleteGroup]. Dedupes to distinct source_texts. */
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
    val sourceTexts = entries.map { it.sourceText }.toSet()
    var totalDeleted = 0
    for (sourceText in sourceTexts) {
      totalDeleted +=
        translationMemoryEntryRepository.deleteByTranslationMemoryIdAndSourceText(
          translationMemoryId = translationMemoryId,
          sourceText = sourceText,
        )
    }
    return totalDeleted
  }

  fun getEntryCount(
    organizationId: Long,
    translationMemoryId: Long,
  ): Long {
    val tm = requireTmInOrganization(organizationId, translationMemoryId)
    val writeProjectIds = writeAccessProjectIds(tm.id)
    return countRows(tm.id, writeProjectIds, tm.writeOnlyReviewed, search = null)
  }

  /**
   * Counts entries for many TMs in one query. Returns a map keyed by TM id; ids the caller
   * isn't allowed to read (different org, deleted) are dropped.
   *
   * The stored half groups `translation_memory_entry` by `(tm_id, source_text, tuid-or-"manual")`
   * and counts groups per tm_id. The virtual half scans every write-access-assigned project
   * once and groups by `(tm_id, source_text, project_id, key_name)` — shared scans across TMs
   * that overlap on projects.
   */
  fun getEntryCounts(
    organizationId: Long,
    translationMemoryIds: List<Long>,
  ): Map<Long, Long> {
    if (translationMemoryIds.isEmpty()) return emptyMap()
    val visibleIds =
      translationMemoryRepository
        .findIdsInOrganization(organizationId, translationMemoryIds)
        .toSet()
    if (visibleIds.isEmpty()) return emptyMap()

    val sql =
      """
      with stored as (
        select e.translation_memory_id as tm_id, count(*) as cnt
        from (
          select translation_memory_id, source_text, coalesce(tuid, 'manual') as bucket
          from translation_memory_entry
          where translation_memory_id = any(:tmIds)
          group by translation_memory_id, source_text, coalesce(tuid, 'manual')
        ) e
        group by e.translation_memory_id
      ),
      virtual as (
        select tm.id as tm_id, count(*) as cnt
        from (
          select tm.id, base_t.text, p.id as project_id, k.name
          from translation_memory tm
          join translation_memory_project tmp
            on tmp.translation_memory_id = tm.id and tmp.write_access = true
          join project p on p.id = tmp.project_id and p.deleted_at is null
          join key k on k.project_id = p.id and k.deleted_at is null
          left join branch b on b.id = k.branch_id
          join translation base_t on base_t.key_id = k.id
                                 and base_t.language_id = p.base_language_id
          where tm.id = any(:tmIds)
            and base_t.text is not null and base_t.text <> ''
            and (b.id is null or b.is_default = true)
            and exists (
              select 1 from translation target_t
              where target_t.key_id = k.id
                and target_t.language_id <> p.base_language_id
                and target_t.text is not null and target_t.text <> ''
                and (not tm.write_only_reviewed or target_t.state = 2)
            )
          group by tm.id, base_t.text, p.id, k.name
        ) tm
        group by tm.id
      )
      select tm_id, sum(cnt) as cnt
      from (
        select tm_id, cnt from stored
        union all
        select tm_id, cnt from virtual
      ) combined
      group by tm_id
      """.trimIndent()

    @Suppress("UNCHECKED_CAST")
    val rows =
      entityManager
        .createNativeQuery(sql)
        .setParameter("tmIds", visibleIds.toTypedArray())
        .resultList as List<Array<Any?>>

    val byId = rows.associate { (it[0] as Number).toLong() to (it[1] as Number).toLong() }
    // TMs with zero entries are absent from both CTEs — surface them as 0 explicitly so the
    // caller distinguishes "not visible" (omitted) from "visible but empty" (0).
    return visibleIds.associateWith { byId[it] ?: 0L }
  }

  private fun requireTmInOrganization(
    organizationId: Long,
    translationMemoryId: Long,
  ): TranslationMemory {
    return translationMemoryRepository.find(organizationId, translationMemoryId)
      ?: throw NotFoundException(Message.TRANSLATION_MEMORY_NOT_FOUND)
  }
}

/** Virtual cell — computed from a project translation rather than stored. */
data class VirtualEntry(
  val sourceText: String,
  val targetText: String,
  val targetLanguageTag: String,
  val projectId: Long,
  val projectName: String,
  val keyName: String,
)
