package io.tolgee.ee.service.translationMemory

import io.tolgee.constants.Message
import io.tolgee.ee.data.translationMemory.CreateTranslationMemoryEntryRequest
import io.tolgee.ee.data.translationMemory.UpdateTranslationMemoryEntryRequest
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
    join language base_lang on base_lang.id = p.base_language_id
    join key k on k.project_id = p.id and k.deleted_at is null
    left join branch b on b.id = k.branch_id
    join translation base_t on base_t.key_id = k.id and base_t.language_id = base_lang.id
    where p.id = any(:projectIds)
      and p.deleted_at is null
      and base_t.text is not null and base_t.text <> ''
      and (b.id is null or b.is_default = true)
      and exists (
        -- EXISTS keeps the join row count at ~|keys| instead of |keys| × |target_langs|.
        -- The search filter must still match a target inside the same bucket, so it lives
        -- here alongside the language/state filters.
        select 1 from translation target_t
        where target_t.key_id = k.id
          and target_t.language_id <> base_lang.id
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
      if (rowId !in rowIds.toSet()) continue
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
  fun findEntriesForTmExport(tm: TranslationMemory): List<TranslationMemoryEntry> {
    val stored = translationMemoryEntryRepository.findByTranslationMemoryId(tm.id)
    val projectIds = writeAccessProjectIds(tm.id)
    if (projectIds.isEmpty()) return stored
    val virtualRows = findAllDistinctVirtualRowsForProjects(projectIds, tm.writeOnlyReviewed)
    val virtual =
      virtualRows.map { row ->
        TranslationMemoryEntry().apply {
          this.translationMemory = tm
          this.sourceText = row[0] as String
          this.targetText = row[1] as String
          this.targetLanguageTag = row[2] as String
        }
      }
    return stored + virtual
  }

  private fun findAllDistinctVirtualRowsForProjects(
    projectIds: List<Long>,
    writeOnlyReviewed: Boolean,
  ): List<Array<Any?>> {
    if (projectIds.isEmpty()) return emptyList()
    val sql =
      """
      select distinct base_t.text as source_text,
             target_t.text as target_text,
             target_lang.tag as target_lang
      from project p
      join language base_lang on base_lang.id = p.base_language_id
      join key k on k.project_id = p.id and k.deleted_at is null
      left join branch b on b.id = k.branch_id
      join translation base_t on base_t.key_id = k.id and base_t.language_id = base_lang.id
      join translation target_t on target_t.key_id = k.id and target_t.language_id <> base_lang.id
      join language target_lang on target_lang.id = target_t.language_id
      where p.id = any(:projectIds)
        and p.deleted_at is null
        and base_t.text is not null and base_t.text <> ''
        and target_t.text is not null and target_t.text <> ''
        and (b.id is null or b.is_default = true)
        and (not :writeOnlyReviewed or target_t.state = 2)
      order by base_t.text, target_lang.tag
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
