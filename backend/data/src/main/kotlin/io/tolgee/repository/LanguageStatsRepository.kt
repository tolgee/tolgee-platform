package io.tolgee.repository

import io.tolgee.dtos.queryResults.LanguageStatsDto
import io.tolgee.model.LanguageStats
import org.springframework.context.annotation.Lazy
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@Lazy
interface LanguageStatsRepository : JpaRepository<LanguageStats, Long> {
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
    select new io.tolgee.dtos.queryResults.LanguageStatsDto(
      ls.language.id,
      ls.language.project.id,
      ls.untranslatedWords,
      ls.translatedWords,
      ls.reviewedWords,
      ls.untranslatedKeys,
      ls.translatedKeys,
      ls.reviewedKeys,
      ls.untranslatedPercentage,
      ls.translatedPercentage,
      ls.reviewedPercentage
    )
    from LanguageStats ls
    where ls.language.project.id in :projectIds and ls.language.deletedAt is null
  """,
  )
  fun getDtosByProjectIds(projectIds: List<Long>): List<LanguageStatsDto>

  @Query(
    """
    from LanguageStats ls
    join fetch ls.language l
    join fetch l.project
    where l.project.id in :projectIds and l.deletedAt is null
  """,
  )
  fun getAllByProjectIds(projectIds: List<Long>): List<LanguageStats>
}
