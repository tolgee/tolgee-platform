package io.tolgee.repository.translationMemory

import io.tolgee.model.translationMemory.TranslationMemoryEntry
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface TranslationMemoryEntryRepository : JpaRepository<TranslationMemoryEntry, Long> {
  fun findByTranslationMemoryId(translationMemoryId: Long): List<TranslationMemoryEntry>

  /**
   * Finds the single entry in [translationMemoryId] matching the `(sourceText, targetText,
   * targetLanguageTag, isManual)` quadruple. Used by both the write pipeline's find-or-create
   * (where `isManual = false`) and the manual-entry create path (where `isManual = true`).
   */
  fun findByTranslationMemoryIdAndSourceTextAndTargetTextAndTargetLanguageTagAndIsManual(
    translationMemoryId: Long,
    sourceText: String,
    targetText: String,
    targetLanguageTag: String,
    isManual: Boolean,
  ): TranslationMemoryEntry?

  /**
   * Returns `[sourceText, targetText, targetLanguageTag]` triples already present in the given TM.
   * Used by the snapshot-on-disconnect path to deduplicate incoming entries without loading the
   * full destination TM as entities.
   */
  @Query(
    """
    select e.sourceText, e.targetText, e.targetLanguageTag
    from TranslationMemoryEntry e
    where e.translationMemory.id = :translationMemoryId
    """,
  )
  fun findDedupKeysByTranslationMemoryId(translationMemoryId: Long): List<Array<Any>>

  /**
   * Paginated list of entries in a TM, optionally filtered by target language tags (comma-separated)
   * and a free-text substring matching source or target text (case-insensitive).
   */
  @Query(
    value = """
      select * from translation_memory_entry e
      where e.translation_memory_id = :translationMemoryId
        and (:targetLanguageTags is null or e.target_language_tag = any(string_to_array(:targetLanguageTags, ',')))
        and (
          :search is null
          or lower(e.source_text::text) like lower('%' || :search || '%')
          or lower(e.target_text::text) like lower('%' || :search || '%')
        )
      order by e.source_text, e.target_language_tag, e.id desc
    """,
    countQuery = """
      select count(*) from translation_memory_entry e
      where e.translation_memory_id = :translationMemoryId
        and (:targetLanguageTags is null or e.target_language_tag = any(string_to_array(:targetLanguageTags, ',')))
        and (
          :search is null
          or lower(e.source_text::text) like lower('%' || :search || '%')
          or lower(e.target_text::text) like lower('%' || :search || '%')
        )
    """,
    nativeQuery = true,
  )
  fun findByTranslationMemoryIdPaged(
    @Param("translationMemoryId") translationMemoryId: Long,
    @Param("targetLanguageTags") targetLanguageTags: String?,
    @Param("search") search: String?,
    pageable: Pageable,
  ): Page<TranslationMemoryEntry>

  /**
   * Returns distinct (source_text, is_manual) group keys in a TM, paginated. Each row is
   * `[source_text, is_manual]`. Manual and synced entries with identical source text live in
   * separate groups so the content browser can render them as separate rows — the keys column
   * (joined through `translation_memory_entry_source`) then differentiates synced (list of
   * contributing keys) from manual (empty).
   */
  @Query(
    value = """
      select distinct e.source_text, e.is_manual
      from translation_memory_entry e
      where e.translation_memory_id = :translationMemoryId
        and (
          :search is null
          or lower(e.source_text::text) like lower('%' || :search || '%')
          or exists (
            select 1 from translation_memory_entry e2
            where e2.translation_memory_id = e.translation_memory_id
              and e2.source_text = e.source_text
              and e2.is_manual = e.is_manual
              and lower(e2.target_text::text) like lower('%' || :search || '%')
          )
        )
      order by e.source_text, e.is_manual desc
    """,
    countQuery = """
      select count(*) from (
        select distinct e.source_text, e.is_manual
        from translation_memory_entry e
        where e.translation_memory_id = :translationMemoryId
          and (
            :search is null
            or lower(e.source_text::text) like lower('%' || :search || '%')
            or exists (
              select 1 from translation_memory_entry e2
              where e2.translation_memory_id = e.translation_memory_id
                and e2.source_text = e.source_text
                and e2.is_manual = e.is_manual
                and lower(e2.target_text::text) like lower('%' || :search || '%')
            )
          )
      ) sub
    """,
    nativeQuery = true,
  )
  fun findDistinctGroupKeysPaged(
    @Param("translationMemoryId") translationMemoryId: Long,
    @Param("search") search: String?,
    pageable: Pageable,
  ): Page<Array<Any?>>

  /**
   * Returns all entries in [translationMemoryId] whose `source_text` is in [sourceTexts] and
   * `is_manual` matches one of the flags present in [isManualFlags]. Optionally narrows by
   * [targetLanguageTags] (comma-separated). Used by the content-browser listing to hydrate the
   * groups returned by [findDistinctGroupKeysPaged].
   */
  @Query(
    value = """
      select * from translation_memory_entry e
      where e.translation_memory_id = :translationMemoryId
        and e.source_text = any(:sourceTexts)
        and (:targetLanguageTags is null or e.target_language_tag = any(string_to_array(:targetLanguageTags, ',')))
      order by e.source_text, e.target_language_tag, e.id desc
    """,
    nativeQuery = true,
  )
  fun findByTranslationMemoryIdAndSourceTexts(
    @Param("translationMemoryId") translationMemoryId: Long,
    @Param("sourceTexts") sourceTexts: Array<String>,
    @Param("targetLanguageTags") targetLanguageTags: String?,
  ): List<TranslationMemoryEntry>

  @Modifying
  @Query("delete from TranslationMemoryEntry e where e.translationMemory.id = :translationMemoryId")
  fun deleteByTranslationMemoryId(translationMemoryId: Long)

  /**
   * Deletes every entry in the TM whose source text matches [sourceText] and whose manual flag
   * matches [isManual]. Used by the bulk "delete entire row" action in the content browser —
   * each visible row corresponds to one `(sourceText, is_manual)` pair. Returns the number of
   * rows deleted.
   */
  @Modifying
  @Query(
    value = """
      delete from translation_memory_entry e
      where e.translation_memory_id = :translationMemoryId
        and e.source_text = :sourceText
        and e.is_manual = :isManual
    """,
    nativeQuery = true,
  )
  fun deleteByTranslationMemoryIdAndSourceTextAndIsManual(
    @Param("translationMemoryId") translationMemoryId: Long,
    @Param("sourceText") sourceText: String,
    @Param("isManual") isManual: Boolean,
  ): Int
}
