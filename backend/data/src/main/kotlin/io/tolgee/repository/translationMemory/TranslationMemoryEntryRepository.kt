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
   * Returns distinct source_texts for stored entries in the TM, paginated. Used by the content
   * browser to lay out one row per distinct source text.
   */
  @Query(
    value = """
      select distinct e.source_text
      from translation_memory_entry e
      where e.translation_memory_id = :translationMemoryId
        and (
          :search is null
          or lower(e.source_text::text) like lower('%' || :search || '%')
          or exists (
            select 1 from translation_memory_entry e2
            where e2.translation_memory_id = e.translation_memory_id
              and e2.source_text = e.source_text
              and lower(e2.target_text::text) like lower('%' || :search || '%')
          )
        )
      order by e.source_text
    """,
    countQuery = """
      select count(*) from (
        select distinct e.source_text
        from translation_memory_entry e
        where e.translation_memory_id = :translationMemoryId
          and (
            :search is null
            or lower(e.source_text::text) like lower('%' || :search || '%')
            or exists (
              select 1 from translation_memory_entry e2
              where e2.translation_memory_id = e.translation_memory_id
                and e2.source_text = e.source_text
                and lower(e2.target_text::text) like lower('%' || :search || '%')
            )
          )
      ) sub
    """,
    nativeQuery = true,
  )
  fun findDistinctSourceTextsPaged(
    @Param("translationMemoryId") translationMemoryId: Long,
    @Param("search") search: String?,
    pageable: Pageable,
  ): Page<String>

  /**
   * Returns all entries in [translationMemoryId] whose `source_text` is in [sourceTexts].
   * Optionally narrows by [targetLanguageTags] (comma-separated).
   *
   * The `md5(...)` predicate is what hits `ix_tm_entry_tm_source` (which indexes the hash, not
   * the raw text, so that long source segments don't overflow the btree row size). The literal
   * `source_text = any(...)` predicate stays as a collision guard — md5 has a ~2^-128 collision
   * probability, but the literal check on the already-narrowed set is cheap insurance.
   */
  @Query(
    value = """
      select * from translation_memory_entry e
      where e.translation_memory_id = :translationMemoryId
        and md5(e.source_text) = any(array(select md5(t) from unnest(:sourceTexts) t))
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
   * Deletes every entry in the TM whose source text matches [sourceText]. Used by the bulk
   * "delete entire row" action in the content browser. Returns the number of rows deleted.
   */
  @Modifying
  @Query(
    value = """
      delete from translation_memory_entry e
      where e.translation_memory_id = :translationMemoryId
        and e.source_text = :sourceText
    """,
    nativeQuery = true,
  )
  fun deleteByTranslationMemoryIdAndSourceText(
    @Param("translationMemoryId") translationMemoryId: Long,
    @Param("sourceText") sourceText: String,
  ): Int
}
