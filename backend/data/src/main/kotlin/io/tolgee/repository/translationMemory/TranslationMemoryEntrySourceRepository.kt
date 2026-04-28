package io.tolgee.repository.translationMemory

import io.tolgee.model.translationMemory.TranslationMemoryEntrySource
import io.tolgee.model.translationMemory.TranslationMemoryEntrySourceId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface TranslationMemoryEntrySourceRepository :
  JpaRepository<TranslationMemoryEntrySource, TranslationMemoryEntrySourceId> {
  /**
   * Returns the synced entry currently associated with [translationId] in [translationMemoryId],
   * if any. A translation belongs to at most one synced entry per TM at any time — the write
   * pipeline detaches before re-attaching whenever the text changes.
   */
  @Query(
    """
    select s.entry
    from TranslationMemoryEntrySource s
    where s.translation.id = :translationId
      and s.entry.translationMemory.id = :translationMemoryId
    """,
  )
  fun findEntryByTranslationIdAndTranslationMemoryId(
    @Param("translationId") translationId: Long,
    @Param("translationMemoryId") translationMemoryId: Long,
  ): io.tolgee.model.translationMemory.TranslationMemoryEntry?

  /**
   * Returns the key names contributing to [entryId] via its source translations. Aggregated for
   * display in the content browser keys column. Empty list when the entry is manual (no sources)
   * or when every contributing translation's key has been deleted.
   */
  @Query(
    """
    select distinct k.name
    from TranslationMemoryEntrySource s
    join s.translation t
    join t.key k
    where s.entry.id = :entryId
    order by k.name
    """,
  )
  fun findKeyNamesByEntryId(
    @Param("entryId") entryId: Long,
  ): List<String>

  /**
   * Batch variant: returns `[entry_id, key_name]` rows for every source of every entry in
   * [entryIds]. Used by the content-browser listing to hydrate the keys column for a page of
   * groups in a single query instead of N+1.
   */
  @Query(
    """
    select s.entry.id, k.name
    from TranslationMemoryEntrySource s
    join s.translation t
    join t.key k
    where s.entry.id in :entryIds
    order by s.entry.id, k.name
    """,
  )
  fun findKeyNamesByEntryIds(
    @Param("entryIds") entryIds: Collection<Long>,
  ): List<Array<Any>>

  fun existsByEntryId(entryId: Long): Boolean

  @Modifying
  @Query(
    """
    delete from TranslationMemoryEntrySource s
    where s.entry.id = :entryId and s.translation.id = :translationId
    """,
  )
  fun deleteByEntryIdAndTranslationId(
    @Param("entryId") entryId: Long,
    @Param("translationId") translationId: Long,
  )
}
