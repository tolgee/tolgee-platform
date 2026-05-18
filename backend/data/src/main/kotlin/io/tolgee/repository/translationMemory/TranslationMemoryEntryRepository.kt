package io.tolgee.repository.translationMemory

import io.tolgee.model.translationMemory.TranslationMemoryEntry
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface TranslationMemoryEntryRepository : JpaRepository<TranslationMemoryEntry, Long> {
  fun findByTranslationMemoryId(translationMemoryId: Long): List<TranslationMemoryEntry>

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
