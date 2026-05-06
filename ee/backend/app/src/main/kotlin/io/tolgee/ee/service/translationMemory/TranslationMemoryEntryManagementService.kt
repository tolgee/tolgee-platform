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
   * Lists rows shown in the TM content browser. One row per distinct source text, with cells
   * for every target language matched by the optional [targetLanguageTag] filter.
   *
   * Page boundary is the union of:
   *  - distinct source_texts of stored entries on this TM, and
   *  - distinct source_texts of writable target translations on every assigned project with
   *    `writeAccess=true`.
   *
   * The language filter only narrows entries within a row; rows still appear for source
   * texts that have no translation in the selected languages.
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
    val writeProjectIds = writeAccessProjectIds(tm.id)

    val total = countGroupKeys(tm.id, writeProjectIds, tm.writeOnlyReviewed, effectiveSearch)
    if (total == 0L) return PageImpl(emptyList(), pageable, 0)

    val pageSourceTexts =
      findGroupSourceTextsPaged(
        tmId = tm.id,
        projectIds = writeProjectIds,
        writeOnlyReviewed = tm.writeOnlyReviewed,
        search = effectiveSearch,
        pageable = pageable,
      )
    if (pageSourceTexts.isEmpty()) return PageImpl(emptyList(), pageable, total)

    val storedBySource = hydrateStoredEntries(tm.id, pageSourceTexts, effectiveLang)
    val virtualBySource =
      hydrateVirtualRows(writeProjectIds, tm.writeOnlyReviewed, pageSourceTexts, effectiveLang)

    val groups =
      pageSourceTexts.map { sourceText ->
        val virtuals = virtualBySource[sourceText].orEmpty()
        TranslationMemoryEntryGroup(
          sourceText = sourceText,
          keyNames = virtuals.map { it.keyName }.distinct().sorted(),
          entries = storedBySource[sourceText].orEmpty(),
          virtualEntries =
            virtuals
              .map {
                VirtualEntry(
                  sourceText = sourceText,
                  targetText = it.targetText,
                  targetLanguageTag = it.targetLang,
                )
              }.distinct(),
        )
      }
    return PageImpl(groups, pageable, total)
  }

  private fun writeAccessProjectIds(tmId: Long): List<Long> =
    translationMemoryProjectRepository
      .findByTranslationMemoryId(tmId)
      .filter { it.writeAccess }
      .map { it.project.id }

  private fun hydrateStoredEntries(
    tmId: Long,
    sourceTexts: List<String>,
    targetLanguageTag: String?,
  ): Map<String, List<TranslationMemoryEntry>> {
    if (sourceTexts.isEmpty()) return emptyMap()
    return translationMemoryEntryRepository
      .findByTranslationMemoryIdAndSourceTexts(
        translationMemoryId = tmId,
        sourceTexts = sourceTexts.toTypedArray(),
        targetLanguageTags = targetLanguageTag,
      ).groupBy { it.sourceText }
  }

  private fun hydrateVirtualRows(
    projectIds: List<Long>,
    writeOnlyReviewed: Boolean,
    sourceTexts: List<String>,
    targetLanguageTag: String?,
  ): Map<String, List<VirtualSourceRow>> {
    if (projectIds.isEmpty() || sourceTexts.isEmpty()) return emptyMap()
    val rows =
      findVirtualRowsForSourceTexts(
        projectIds = projectIds,
        writeOnlyReviewed = writeOnlyReviewed,
        sourceTexts = sourceTexts,
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

  private fun countGroupKeys(
    tmId: Long,
    projectIds: List<Long>,
    writeOnlyReviewed: Boolean,
    search: String?,
  ): Long {
    val sql = "select count(distinct source_text) from (${groupKeysUnionSql()}) sub"
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

  private fun findGroupSourceTextsPaged(
    tmId: Long,
    projectIds: List<Long>,
    writeOnlyReviewed: Boolean,
    search: String?,
    pageable: Pageable,
  ): List<String> {
    val sql =
      """
      select distinct source_text from (${groupKeysUnionSql()}) sub
      order by source_text
      limit :limit offset :offset
      """.trimIndent()
    @Suppress("UNCHECKED_CAST")
    return (
      entityManager
        .createNativeQuery(sql)
        .setParameter("tmId", tmId)
        .setParameter("projectIds", projectIds.toTypedArray())
        .setParameter("writeOnlyReviewed", writeOnlyReviewed)
        .setParameter("search", search)
        .setParameter("limit", pageable.pageSize)
        .setParameter("offset", pageable.offset)
        .resultList as List<Any?>
    ).map { it as String }
  }

  /**
   * SQL fragment producing distinct source_texts from:
   *  - stored entries on this TM, and
   *  - target translations on every project assigned with writeAccess.
   *
   * Empty `:projectIds` reduces the second half to zero rows so only stored entries surface,
   * which is the right fallback for a TM with no write-assigned projects.
   */
  private fun groupKeysUnionSql(): String =
    """
    select e.source_text as source_text
    from translation_memory_entry e
    where e.translation_memory_id = :tmId
      and (
        cast(:search as text) is null
        or lower(e.source_text::text) like lower('%' || cast(:search as text) || '%')
        or exists (
          select 1 from translation_memory_entry e2
          where e2.translation_memory_id = e.translation_memory_id
            and e2.source_text = e.source_text
            and lower(e2.target_text::text) like lower('%' || cast(:search as text) || '%')
        )
      )

    union

    select base_t.text as source_text
    from project p
    join language base_lang on base_lang.id = p.base_language_id
    join key k on k.project_id = p.id and k.deleted_at is null
    left join branch b on b.id = k.branch_id
    join translation base_t on base_t.key_id = k.id and base_t.language_id = base_lang.id
    join translation target_t on target_t.key_id = k.id and target_t.language_id <> base_lang.id
    where p.id = any(:projectIds)
      and p.deleted_at is null
      and base_t.text is not null and base_t.text <> ''
      and target_t.text is not null and target_t.text <> ''
      and (b.id is null or b.is_default = true)
      and (not :writeOnlyReviewed or target_t.state = 2)
      and (
        cast(:search as text) is null
        or lower(base_t.text) like lower('%' || cast(:search as text) || '%')
        or lower(target_t.text) like lower('%' || cast(:search as text) || '%')
      )
    """.trimIndent()

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

  private fun findVirtualRowsForSourceTexts(
    projectIds: List<Long>,
    writeOnlyReviewed: Boolean,
    sourceTexts: List<String>,
    targetLanguageTag: String?,
  ): List<Array<Any?>> {
    if (projectIds.isEmpty() || sourceTexts.isEmpty()) return emptyList()
    val sql =
      """
      select base_t.text as source_text,
             target_t.text as target_text,
             target_lang.tag as target_lang,
             k.name as key_name
      from project p
      join language base_lang on base_lang.id = p.base_language_id
      join key k on k.project_id = p.id and k.deleted_at is null
      left join branch b on b.id = k.branch_id
      join translation base_t on base_t.key_id = k.id and base_t.language_id = base_lang.id
      join translation target_t on target_t.key_id = k.id and target_t.language_id <> base_lang.id
      join language target_lang on target_lang.id = target_t.language_id
      where p.id = any(:projectIds)
        and p.deleted_at is null
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
      .setParameter("projectIds", projectIds.toTypedArray())
      .setParameter("writeOnlyReviewed", writeOnlyReviewed)
      .setParameter("sourceTexts", sourceTexts.toTypedArray())
      .setParameter("targetLanguageTags", targetLanguageTag)
      .resultList as List<Array<Any?>>
  }

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
)
