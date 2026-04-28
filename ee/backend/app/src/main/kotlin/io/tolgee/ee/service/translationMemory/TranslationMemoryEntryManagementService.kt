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
 * are editable by the user via the add-entry dialog and TMX import. Synced entries
 * (`is_manual = false`) are written by the translation-save pipeline and are read-only here —
 * [update] and [delete] refuse them. Virtual entries (project-TM content computed from the
 * project's translations) are not represented as rows at all; they appear only in
 * [listEntryGroups] output.
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
   * SHARED TMs are read from `translation_memory_entry`. PROJECT TMs hold no stored entries by
   * design — their content is computed virtually from the project's translations on every read,
   * and pagination is applied at the SQL level over distinct source texts.
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

    val projectId = projectIdForTm(tm)
      ?: return PageImpl(emptyList(), pageable, 0)
    return loadVirtualGroupsPaged(
      tm = tm,
      projectId = projectId,
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
   * Paginates project-TM virtual content at the SQL level. The earlier implementation pulled every
   * virtual row into memory and concatenated them onto each requested page — so projects with many
   * translations rendered the same content infinitely as the user scrolled. Now the page boundary
   * is the distinct source-text list, and only the rows for the current page's source texts are
   * hydrated.
   */
  private fun loadVirtualGroupsPaged(
    tm: TranslationMemory,
    projectId: Long,
    search: String?,
    targetLanguageTag: String?,
    pageable: Pageable,
  ): Page<TranslationMemoryEntryGroup> {
    val total = countDistinctVirtualSourceTexts(projectId, tm.writeOnlyReviewed, search)
    if (total == 0L) {
      return PageImpl(emptyList(), pageable, 0)
    }

    val pageSourceTexts =
      findDistinctVirtualSourceTextsPaged(
        projectId = projectId,
        writeOnlyReviewed = tm.writeOnlyReviewed,
        search = search,
        pageable = pageable,
      )
    if (pageSourceTexts.isEmpty()) {
      return PageImpl(emptyList(), pageable, total)
    }

    val rows =
      findVirtualRowsForSourceTexts(
        projectId = projectId,
        writeOnlyReviewed = tm.writeOnlyReviewed,
        sourceTexts = pageSourceTexts,
        targetLanguageTag = targetLanguageTag,
      )

    data class VirtualRow(val targetText: String, val targetLang: String, val keyName: String)

    val bySource = mutableMapOf<String, MutableList<VirtualRow>>()
    for (row in rows) {
      val sourceText = row[0] as String
      val targetText = row[1] as String
      val targetLang = row[2] as String
      val keyName = row[3] as String
      bySource.getOrPut(sourceText) { mutableListOf() }.add(VirtualRow(targetText, targetLang, keyName))
    }

    // Preserve the page-query's source-text ordering so paged output is stable across requests.
    val groups =
      pageSourceTexts.map { sourceText ->
        val srcRows = bySource[sourceText].orEmpty()
        val keyNames = srcRows.map { it.keyName }.distinct().sorted()
        val virtualEntries =
          srcRows
            .map {
              VirtualEntry(
                sourceText = sourceText,
                targetText = it.targetText,
                targetLanguageTag = it.targetLang,
              )
            }.distinct()
        TranslationMemoryEntryGroup(
          sourceText = sourceText,
          keyNames = keyNames,
          isManual = false,
          entries = emptyList(),
          virtualEntries = virtualEntries,
        )
      }
    return PageImpl(groups, pageable, total)
  }

  private fun countDistinctVirtualSourceTexts(
    projectId: Long,
    writeOnlyReviewed: Boolean,
    search: String?,
  ): Long {
    val sql =
      """
      select count(distinct base_t.text)
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
      """.trimIndent()
    val result =
      entityManager
        .createNativeQuery(sql)
        .setParameter("projectId", projectId)
        .setParameter("writeOnlyReviewed", writeOnlyReviewed)
        .setParameter("search", search)
        .singleResult
    return (result as Number).toLong()
  }

  private fun findDistinctVirtualSourceTextsPaged(
    projectId: Long,
    writeOnlyReviewed: Boolean,
    search: String?,
    pageable: Pageable,
  ): List<String> {
    val sql =
      """
      select distinct base_t.text as source_text
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
      order by source_text
      limit :limit offset :offset
      """.trimIndent()
    @Suppress("UNCHECKED_CAST")
    val rows =
      entityManager
        .createNativeQuery(sql)
        .setParameter("projectId", projectId)
        .setParameter("writeOnlyReviewed", writeOnlyReviewed)
        .setParameter("search", search)
        .setParameter("limit", pageable.pageSize)
        .setParameter("offset", pageable.offset)
        .resultList as List<String>
    return rows
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
