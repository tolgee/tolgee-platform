package io.tolgee.repository

import io.tolgee.dtos.cacheable.LanguageDto
import io.tolgee.dtos.cacheable.OrganizationLanguageDto
import io.tolgee.dtos.request.language.LanguageFilters
import io.tolgee.model.Language
import org.springframework.context.annotation.Lazy
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.*

const val LANGUAGE_FILTERS = """
    (
        :#{#filters.filterId} is null
        or l.id in :#{#filters.filterId}
    )
    and (
        :#{#filters.filterNotId} is null
        or l.id not in :#{#filters.filterNotId}
    )
"""

const val SEARCH_FILTER = """
    (
        :search is null or (lower(l.name) like lower(concat('%', cast(:search as text), '%'))
        or lower(l.tag) like lower(concat('%', cast(:search as text),'%')))
    )
"""

const val ORGANIZATION_FILTER = """
    o.id = :organizationId
    and o.deleted_at is null
    and p.deleted_at is null
    and l.deleted_at is null
"""

@Repository
@Lazy
interface LanguageRepository : JpaRepository<Language, Long> {
  @Query(
    """
    select l
    from Language l
    where l.name = :name and l.project = :project and l.deletedAt is null
  """,
  )
  fun findByNameAndProject(
    name: String?,
    project: io.tolgee.model.Project,
  ): Optional<Language>

  @Query(
    """
    select l
    from Language l
    where l.project.id = :projectId and l.deletedAt is null
  """,
  )
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
      coalesce((l.id = l.project.baseLanguage.id), false) as base
    )
    from Language l
    where l.project.id = :projectId and l.deletedAt is null
        and $LANGUAGE_FILTERS
  """,
  )
  fun findAllByProjectId(
    projectId: Long?,
    pageable: Pageable,
    filters: LanguageFilters,
  ): Page<LanguageDto>

  @Query(
    """
    select l
    from Language l
    where l.tag in :tag and l.project.id = :projectId and l.deletedAt is null
  """,
  )
  fun findAllByTagInAndProjectId(
    tag: Collection<String>,
    projectId: Long,
  ): List<Language>

  fun deleteAllByProjectId(projectId: Long?)

  @Query(
    """
    select l
    from Language l
    where l.project.id = :projectId and l.id in :languageIds and l.deletedAt is null
  """,
  )
  fun findAllByProjectIdAndIds(
    projectId: Long,
    languageIds: List<Long>,
  ): List<Language>

  @Query(
    """
    with base_distinct_tags AS (
      select min(l.id) as id, l.tag as tag
      from language l
      join project p on p.id = l.project_id
      join organization o on p.organization_owner_id = o.id
      where $ORGANIZATION_FILTER
      and l.id = p.base_language_id
      and $SEARCH_FILTER
      group by l.tag
    ),
    non_base_distinct_tags AS (
      select min(l.id) as id, l.tag as tag
      from language l
      join project p on p.id = l.project_id
      join organization o on p.organization_owner_id = o.id
      where $ORGANIZATION_FILTER
      and l.id != p.base_language_id
      and $SEARCH_FILTER
      and l.tag not in (
        select tag
        from base_distinct_tags
      )
      group by l.tag
    )
    select *
    from (
      select
        l.name as name,
        l.tag as tag,
        l.original_name as originalName,
        l.flag_emoji as flagEmoji,
        (
          CASE
            WHEN l.id IN (SELECT id FROM base_distinct_tags) THEN true
            ELSE false
          END
        ) as base
      from language l
      where l.id in (
          select id from base_distinct_tags
        )
        or l.id in (
          select id from non_base_distinct_tags
        )
    ) as result
    """,
    nativeQuery = true,
  )
  fun findAllByOrganizationId(
    organizationId: Long?,
    pageable: Pageable,
    search: String?,
  ): Page<OrganizationLanguageDto>

  @Query(
    """
    select l
    from Language l
    where l.project.id = :projectId and l.id in :languageId and l.deletedAt is null
  """,
  )
  fun find(
    languageId: Long,
    projectId: Long,
  ): Language?

  @Query(
    """
    select l
    from Language l
    where l.id = :languageId and l.deletedAt is null
  """,
  )
  fun find(languageId: Long): Language?

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
    from Language l where l.project.id = :projectId and l.deletedAt is null
  """,
  )
  fun findAllDtosByProjectId(projectId: Long): List<LanguageDto>
}
