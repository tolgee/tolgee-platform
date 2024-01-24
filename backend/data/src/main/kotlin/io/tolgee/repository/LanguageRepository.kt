package io.tolgee.repository

import io.tolgee.dtos.cacheable.LanguageDto
import io.tolgee.model.Language
import io.tolgee.model.Project
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface LanguageRepository : JpaRepository<Language, Long> {
  fun findByNameAndProject(
    name: String?,
    project: Project,
  ): Optional<Language>

  fun findAllByProjectId(projectId: Long?): Set<Language>

  @Query(
    """
    select new io.tolgee.dtos.cacheable.LanguageDto(
      l.id,
      l.name,
      l.tag,
      l.originalName,
      l.flagEmoji,
      l.aiTranslatorPromptDescription,
      coalesce((l.id = l.project.baseLanguage.id), false)
    )
    from Language l
    where l.project.id = :projectId
  """,
  )
  fun findAllByProjectId(
    projectId: Long?,
    pageable: Pageable,
  ): Page<LanguageDto>

  fun findAllByTagInAndProjectId(
    tag: Collection<String>,
    projectId: Long,
  ): List<Language>

  fun deleteAllByProjectId(projectId: Long?)

  fun findAllByProjectIdAndIdInOrderById(
    projectId: Long,
    languageIds: List<Long>,
  ): List<Language>

  @Query(
    """
    select l
    from Language l
    where l.project.id = :projectId and l.id in :languageId
  """,
  )
  fun find(
    languageId: Long,
    projectId: Long,
  ): Language?

  @Query(
    """
    select new io.tolgee.dtos.cacheable.LanguageDto(
      l.id,
      l.name,
      l.tag,
      l.originalName,
      l.flagEmoji,
      l.aiTranslatorPromptDescription,
      coalesce((l.id = l.project.baseLanguage.id), false)
    )
    from Language l where l.project.id = :projectId
  """,
  )
  fun findAllDtosByProjectId(projectId: Long): List<LanguageDto>

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
