package io.tolgee.repository

import io.tolgee.model.Language
import io.tolgee.model.Project
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
    project: Project,
  ): Optional<Language>

  fun findByNameAndProject(
    name: String?,
    project: Project,
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

  @Query(
    """
      SELECT new map(t.id as translationId, t.language.id as languageId)
      FROM Translation t
      WHERE t.id IN :translationIds
    """
  )
  fun findLanguageIdsOfTranslations(translationIds: List<Long>): List<Map<String, Long>>

  @Query("SELECT l.id FROM Language l, Project p WHERE p = :project AND l = p.baseLanguage")
  fun getBaseLanguageForProject(project: Project): Long?
}
