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
import java.util.Optional

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
    value = """
      select distinct l.tag as result
      from language l
      join project p on p.id = l.project_id
      join organization o on o.id = p.organization_owner_id
      where $ORGANIZATION_FILTER
    """,
    countQuery = """
      select count(distinct l.tag) as result
      from language l
      join project p on p.id = l.project_id
      join organization o on o.id = p.organization_owner_id
      where $ORGANIZATION_FILTER
    """,
    nativeQuery = true,
  )
  fun findAllTagsByOrganizationId(
    organizationId: Long?,
    projectIds: List<Long>,
    anyProject: Boolean,
  ): Set<String>

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
    value = """
      select *
      from (
        select distinct on (l.tag)
          l.name as name,
          l.tag as tag,
          l.original_name as originalName,
          l.flag_emoji as flagEmoji,
          (l.id = p.base_language_id) as base
        from language l
        join project p on p.id = l.project_id
        join organization o on o.id = p.organization_owner_id
        where $ORGANIZATION_FILTER
          and $SEARCH_FILTER
        order by l.tag, (l.id = p.base_language_id) desc, l.id
      ) as result
    """,
    countQuery = """
      select count(distinct l.tag) as result
      from language l
      join project p on p.id = l.project_id
      join organization o on o.id = p.organization_owner_id
        where $ORGANIZATION_FILTER
          and $SEARCH_FILTER
    """,
    nativeQuery = true,
  )
  fun findAllByOrganizationId(
    organizationId: Long?,
    projectIds: List<Long>,
    anyProject: Boolean,
    pageable: Pageable,
    search: String?,
  ): Page<OrganizationLanguageDto>

  @Query(
    value = """
      select *
      from (
        select distinct on (l.tag)
          l.name as name,
          l.tag as tag,
          l.original_name as originalName,
          l.flag_emoji as flagEmoji,
          true as base
        from language l
        join project p on p.base_language_id = l.id
        join organization o on o.id = p.organization_owner_id
        where $ORGANIZATION_FILTER
          and $SEARCH_FILTER
        order by l.tag, l.id
      ) as result
    """,
    countQuery = """
      select count(distinct l.tag) as result
      from language l
      join project p on p.base_language_id = l.id
      join organization o on o.id = p.organization_owner_id
        where $ORGANIZATION_FILTER
          and $SEARCH_FILTER
    """,
    nativeQuery = true,
  )
  fun findAllBaseByOrganizationId(
    organizationId: Long?,
    projectIds: List<Long>,
    anyProject: Boolean,
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
    select l
    from Language l
    where l.project.id = :projectId and l.tag = :languageTag and l.deletedAt is null
  """,
  )
  fun find(
    projectId: Long,
    languageTag: String,
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
    from Language l where l.project.id = :projectId and l.deletedAt is null
  """,
  )
  fun findAllDtosByProjectId(projectId: Long): List<LanguageDto>

  companion object {
    const val LANGUAGE_FILTERS = """
      (
        (
            :#{#filters.filterId} is null
            or l.id in :#{#filters.filterId}
        )
        and (
            :#{#filters.filterNotId} is null
            or l.id not in :#{#filters.filterNotId}
        )
      )
"""

    const val SEARCH_FILTER = """
      (
          :search is null or (lower(l.name) like lower(concat('%', cast(:search as text), '%'))
          or lower(l.tag) like lower(concat('%', cast(:search as text),'%')))
      )
"""

    const val ORGANIZATION_FILTER = """
      (
        o.id = :organizationId
        and (:anyProject or l.project_id in :projectIds)
        and o.deleted_at is null
        and p.deleted_at is null
        and l.deleted_at is null
      )
"""
  }
}
