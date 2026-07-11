package io.tolgee.ee.service.translationMemory

import io.tolgee.constants.Message
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.translationMemory.TranslationMemory
import io.tolgee.repository.translationMemory.TranslationMemoryEntryRepository
import io.tolgee.repository.translationMemory.TranslationMemoryProjectRepository
import io.tolgee.repository.translationMemory.TranslationMemoryRepository
import jakarta.persistence.EntityManager
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

/**
 * Read-side service for the TM content browser: paginates rows (mixed stored + virtual),
 * resolves entry ids for "select all", and answers the per-TM entry-count query the org
 * list view renders next to each TM.
 *
 * Mutations live on [TranslationMemoryEntryManagementService]; TMX export lives on
 * [io.tolgee.ee.service.translationMemory.TranslationMemoryTmxService].
 */
@Service
class TranslationMemoryRowListingService(
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
    translationMemoryProjectRepository.findWriteAccessProjectIds(tmId)

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
   *
   * Maps each JPA `TranslationMemoryEntry` to a [TmStoredCell] right here so the JPA model
   * never escapes the service — the controller-side assembler reads only DTO fields.
   */
  private fun hydrateStoredRows(
    tmId: Long,
    rowIds: List<RowId>,
    targetLanguageTag: String?,
  ): Map<RowId, List<TmStoredCell>> {
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
      .mapValues { (_, group) ->
        group.map { entry ->
          TmStoredCell(
            entryId = entry.id,
            targetText = entry.targetText,
            targetLanguageTag = entry.targetLanguageTag,
          )
        }
      }
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

  private fun requireTmInOrganization(
    organizationId: Long,
    translationMemoryId: Long,
  ): TranslationMemory {
    return translationMemoryRepository.find(organizationId, translationMemoryId)
      ?: throw NotFoundException(Message.TRANSLATION_MEMORY_NOT_FOUND)
  }
}
