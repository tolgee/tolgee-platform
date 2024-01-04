package io.tolgee.repository

import io.tolgee.model.Language
import io.tolgee.model.views.LanguageView
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface LanguageRepository : JpaRepository<Language, Long> {
  fun findByTagAndProject(
    abbreviation: String,
    project: io.tolgee.model.Project,
  ): Optional<Language>

  fun findByNameAndProject(
    name: String?,
    project: io.tolgee.model.Project,
  ): Optional<Language>

  fun findByTagAndProjectId(
    abbreviation: String?,
    projectId: Long,
  ): Optional<Language>

  fun findAllByProjectId(projectId: Long?): Set<Language>

  @Query(
    """
    select l as language, (pb.id = l.id) as base 
    from Language l
      join l.project p
      left join p.baseLanguage pb
    where l.project.id = :projectId
  """,
  )
  fun findAllByProjectId(
    projectId: Long?,
    pageable: Pageable,
  ): Page<LanguageView>

  fun findAllByTagInAndProjectId(
    abbreviation: Collection<String?>?,
    projectId: Long?,
  ): List<Language>

  fun deleteAllByProjectId(projectId: Long?)

  @Query(
    """
    select l as language, (l.id = bl.id) as base
    from Language l
    join l.project p
    join p.baseLanguage bl
    where l.id = :id
  """,
  )
  fun findView(id: Long): LanguageView?

  @Query(
    """
    select l as language, (l.id = coalesce(bl.id, 0)) as base
    from Language l
    join l.project p
    left join p.baseLanguage bl
    where l.project.id in :projectIds
  """,
  )
  fun getViewsOfProjects(projectIds: List<Long>): List<LanguageView>

  fun countByIdInAndProjectId(
    languageIds: Set<Long>,
    projectId: Long,
  ): Int

  fun findAllByProjectIdAndIdInOrderById(
    projectId: Long,
    languageIds: List<Long>,
  ): List<Language>

  fun findByIdAndProjectId(
    id: Long,
    projectId: Long,
  ): Language?
}
