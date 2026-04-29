package io.tolgee.repository.translationMemory

import io.tolgee.model.translationMemory.TranslationMemoryProject
import jakarta.persistence.QueryHint
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.QueryHints
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
}
