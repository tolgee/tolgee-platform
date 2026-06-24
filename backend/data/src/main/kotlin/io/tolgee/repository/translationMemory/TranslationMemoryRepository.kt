package io.tolgee.repository.translationMemory

import io.tolgee.model.translationMemory.TranslationMemory
import io.tolgee.model.translationMemory.TranslationMemoryWithStats
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface TranslationMemoryRepository : JpaRepository<TranslationMemory, Long> {
  @Query(
    """
    from TranslationMemory
    where organizationOwner.id = :organizationId
      and organizationOwner.deletedAt is null
      and id = :translationMemoryId
    """,
  )
  fun find(
    organizationId: Long,
    translationMemoryId: Long,
  ): TranslationMemory?

  @Query(
    """
    from TranslationMemory
    where organizationOwner.id = :organizationId
      and organizationOwner.deletedAt is null
    """,
  )
  fun findByOrganizationId(organizationId: Long): List<TranslationMemory>

  @Query(
    """
    select tm.id
    from TranslationMemory tm
    where tm.organizationOwner.id = :organizationId
      and tm.organizationOwner.deletedAt is null
      and tm.id in :translationMemoryIds
    """,
  )
  fun findIdsInOrganization(
    organizationId: Long,
    translationMemoryIds: List<Long>,
  ): List<Long>

  @Query(
    """
    from TranslationMemory
    where organizationOwner.id = :organizationId
      and organizationOwner.deletedAt is null
      and (lower(name) like lower(concat('%', coalesce(:search, ''), '%')) or :search is null)
    """,
  )
  fun findByOrganizationIdPaged(
    organizationId: Long,
    pageable: Pageable,
    search: String?,
  ): Page<TranslationMemory>

  @Query(
    """
    select distinct tm
    from TranslationMemory tm
      join tm.projectAssignments pa
    where pa.project.id = :projectId
      and tm.organizationOwner.deletedAt is null
    order by tm.name
    """,
  )
  fun findAssignedToProject(projectId: Long): List<TranslationMemory>

  /**
   * Returns true if another TM in the same organization already uses [name]. [excludeId] is
   * the id of the TM being updated (so it doesn't conflict with itself); pass null on create.
   * Name matching is case-insensitive — "Marketing" and "marketing" collide.
   */
  @Query(
    """
    select count(tm) > 0 from TranslationMemory tm
    where tm.organizationOwner.id = :organizationId
      and tm.organizationOwner.deletedAt is null
      and lower(tm.name) = lower(:name)
      and (:excludeId is null or tm.id <> :excludeId)
    """,
  )
  fun existsByOrganizationIdAndName(
    organizationId: Long,
    name: String,
    excludeId: Long?,
  ): Boolean

  @Query(
    value = """
      select tm.id as id,
             tm.name as name,
             tm.source_language_tag as sourceLanguageTag,
             tm.type as type,
             pn.names as assignedProjectNamesCsv,
             tm.default_penalty as defaultPenalty,
             tm.write_only_reviewed as writeOnlyReviewed
      from translation_memory tm
      left join lateral (
        select string_agg(pr.name, ',' order by pr.name) as names
        from translation_memory_project tp
        join project pr on pr.id = tp.project_id
        where tp.translation_memory_id = tm.id
      ) pn on true
      where tm.organization_owner_id = :organizationId
        and not exists (
          select 1 from organization o
          where o.id = tm.organization_owner_id and o.deleted_at is not null
        )
        and (:search is null or lower(tm.name) like lower(concat('%', :search, '%')))
        and (:type is null or tm.type = :type)
      order by case when tm.type = 'SHARED' then 0 else 1 end, tm.id desc
    """,
    countQuery = """
      select count(*)
      from translation_memory tm
      where tm.organization_owner_id = :organizationId
        and not exists (
          select 1 from organization o
          where o.id = tm.organization_owner_id and o.deleted_at is not null
        )
        and (:search is null or lower(tm.name) like lower(concat('%', :search, '%')))
        and (:type is null or tm.type = :type)
    """,
    nativeQuery = true,
  )
  fun findByOrganizationIdWithStatsPaged(
    organizationId: Long,
    pageable: Pageable,
    search: String?,
    type: String?,
  ): Page<TranslationMemoryWithStats>
}
