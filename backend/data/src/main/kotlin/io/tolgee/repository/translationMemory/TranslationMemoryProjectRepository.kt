package io.tolgee.repository.translationMemory

import io.tolgee.model.translationMemory.TranslationMemoryProject
import io.tolgee.model.translationMemory.TranslationMemoryType
import jakarta.persistence.QueryHint
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.QueryHints
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface TranslationMemoryProjectRepository : JpaRepository<TranslationMemoryProject, Long> {
  fun findByProjectId(projectId: Long): List<TranslationMemoryProject>

  /**
   * Used by the suggestion/auto-translate read path. Marked with FLUSH_MODE=COMMIT for the same
   * reason as [findByProjectIdAndWriteAccessTrue] — callers may invoke this while an outer save
   * is mid-flight; we don't want the query to trigger a premature flush that exposes transient
   * entities on the in-progress Translation.
   */
  @QueryHints(
    value = [QueryHint(name = "org.hibernate.flushMode", value = "COMMIT")],
  )
  fun findByProjectIdAndReadAccessTrue(projectId: Long): List<TranslationMemoryProject>

  /**
   * Used by the write-on-save hook ([io.tolgee.service.translationMemory.TranslationMemoryEntryService.onTranslationSaved]).
   * Marked with FLUSH_MODE=COMMIT so running this query inside the translation save flow does NOT trigger a
   * premature Hibernate flush — flushing from within the save hook would expose transient entities that are
   * still being assembled by the outer save (e.g. Labels on the enclosing Translation).
   */
  @QueryHints(
    value = [QueryHint(name = "org.hibernate.flushMode", value = "COMMIT")],
  )
  fun findByProjectIdAndWriteAccessTrue(projectId: Long): List<TranslationMemoryProject>

  fun findByTranslationMemoryId(translationMemoryId: Long): List<TranslationMemoryProject>

  fun findByTranslationMemoryIdAndProjectId(
    translationMemoryId: Long,
    projectId: Long,
  ): TranslationMemoryProject?

  fun deleteByProjectId(projectId: Long)

  /**
   * Returns `(project_id, max_priority)` pairs for the given project ids. Used to compute the
   * "next available priority" for many new shared-TM assignments in one DB round-trip instead
   * of one per assigned project.
   */
  @Query(
    """
    select p.project.id, max(p.priority)
    from TranslationMemoryProject p
    where p.project.id in :projectIds
    group by p.project.id
    """,
  )
  fun findMaxPriorityByProjectIds(
    @Param("projectIds") projectIds: Collection<Long>,
  ): List<Array<Any>>

  /**
   * Projection-only query for the suggestion read path: returns just the readable TM ids,
   * optionally filtered by [type]. Avoids hydrating the `TranslationMemory` entity per
   * assignment (which the previous "load all + filter in Kotlin" implementation did even
   * when only the project TM mattered, e.g. on free plan).
   *
   * Marked with `FLUSH_MODE=COMMIT` for the same reason as [findByProjectIdAndReadAccessTrue]:
   * this can be invoked mid-translation-save and must not trigger a premature flush.
   */
  @QueryHints(
    value = [QueryHint(name = "org.hibernate.flushMode", value = "COMMIT")],
  )
  @Query(
    """
    select p.translationMemory.id
    from TranslationMemoryProject p
    where p.project.id = :projectId
      and p.readAccess = true
      and (:type is null or p.translationMemory.type = :type)
    """,
  )
  fun findReadableTmIdsByProjectId(
    @Param("projectId") projectId: Long,
    @Param("type") type: TranslationMemoryType?,
  ): List<Long>
}
