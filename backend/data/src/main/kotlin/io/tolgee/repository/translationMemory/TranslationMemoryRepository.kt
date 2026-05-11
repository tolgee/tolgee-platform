package io.tolgee.repository.translationMemory

import io.tolgee.model.translationMemory.TranslationMemory
import io.tolgee.model.translationMemory.TranslationMemoryType
import io.tolgee.model.translationMemory.TranslationMemoryWithStats
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface TranslationMemoryRepository : JpaRepository<TranslationMemory, Long> {
  fun findByOrganizationOwnerIdAndType(
    organizationOwnerId: Long,
    type: TranslationMemoryType,
  ): List<TranslationMemory>

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
    from TranslationMemory
    where organizationOwner.id = :organizationId
      and organizationOwner.deletedAt is null
      and type = :type
      and (lower(name) like lower(concat('%', coalesce(:search, ''), '%')) or :search is null)
    """,
  )
  fun findByOrganizationIdAndTypePaged(
    organizationId: Long,
    type: TranslationMemoryType,
    pageable: Pageable,
    search: String?,
  ): Page<TranslationMemory>

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
             coalesce(ec.cnt, 0) as entryCount,
             pn.names as assignedProjectNamesCsv,
             tm.default_penalty as defaultPenalty,
             tm.write_only_reviewed as writeOnlyReviewed
      from translation_memory tm
      left join lateral (
        -- Row count, matching TranslationMemoryEntryManagementService.rowIdentitiesSql:
        --   - Stored: one row per (source_text, tuid-or-"manual") bucket. Each TMX tuid is
        --     its own row; all null-tuid manual entries on a source collapse into one row.
        --   - Virtual: one row per (source_text, project_id, key_name) on every project
        --     assigned with write_access = true.
        select
          (
            select count(*) from (
              select 1
              from translation_memory_entry e
              where e.translation_memory_id = tm.id
              group by e.source_text, coalesce(e.tuid, 'manual')
            ) stored_rows
          )
          +
          (
            select count(*) from (
              select 1
              from translation_memory_project tmp
              join project p on p.id = tmp.project_id and p.deleted_at is null
              join language base_lang on base_lang.id = p.base_language_id
              join key k on k.project_id = p.id and k.deleted_at is null
              left join branch b on b.id = k.branch_id
              join translation base_t on base_t.key_id = k.id
                                     and base_t.language_id = base_lang.id
              join translation target_t on target_t.key_id = k.id
                                       and target_t.language_id <> base_lang.id
              where tmp.translation_memory_id = tm.id
                and tmp.write_access = true
                and base_t.text is not null and base_t.text <> ''
                and target_t.text is not null and target_t.text <> ''
                and (b.id is null or b.is_default = true)
                and (not tm.write_only_reviewed or target_t.state = 2)
              group by base_t.text, p.id, k.name
            ) virtual_rows
          ) as cnt
      ) ec on true
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
