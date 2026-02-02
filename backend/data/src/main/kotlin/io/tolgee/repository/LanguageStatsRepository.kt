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
      ls.reviewedPercentage,
      ls.translationsUpdatedAt,
      b.isDefault
    )
    from LanguageStats ls
    left join ls.branch b
    where ls.language.project.id = :projectId
      and ls.language.deletedAt is null
      and ((b.id = :branchId and b.deletedAt is null) or (:branchId is null and b is null))
  """,
  )
  fun getDtosByProjectIdAndBranchId(
    projectId: Long,
    branchId: Long?,
  ): List<LanguageStatsDto>

  @Query(
    """
    from LanguageStats ls
    join fetch ls.language l
    join fetch l.project
    left join ls.branch b
    where l.project.id = :projectId
      and l.deletedAt is null
      and ((b.id = :branchId and b.deletedAt is null) or (:branchId is null and (b is null or b.isDefault)))
  """,
  )
  fun getAllByProjectIdAndBranchId(
    projectId: Long,
    branchId: Long?,
  ): List<LanguageStats>
}
