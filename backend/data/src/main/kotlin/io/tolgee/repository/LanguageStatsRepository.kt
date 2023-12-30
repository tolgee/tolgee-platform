package io.tolgee.repository

import io.tolgee.model.LanguageStats
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
interface LanguageStatsRepository : JpaRepository<LanguageStats, Long> {
  @Query(
    """
    from LanguageStats ls
    join ls.language l
    where l.project.id = :projectId
  """,
  )
  fun getAllByProjectId(projectId: Long): List<LanguageStats>

  @Modifying
  @Transactional
  @Query(
    """
    delete from LanguageStats ls where ls.language.id = :languageId
  """,
  )
  fun deleteAllByLanguage(languageId: Long)

  @Query(
    """
    from LanguageStats ls
    join fetch ls.language l
    join fetch l.project
    where l.project.id in :projectIds
  """,
  )
  fun getAllByProjectIds(projectIds: List<Long>): List<LanguageStats>
}
