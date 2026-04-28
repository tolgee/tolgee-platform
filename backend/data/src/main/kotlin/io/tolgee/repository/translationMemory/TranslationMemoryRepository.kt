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

  @Query(
    value = """
      select tm.id as id,
             tm.name as name,
             tm.source_language_tag as sourceLanguageTag,
             tm.type as type,
             coalesce(ec.cnt, 0) as entryCount,
             coalesce(pc.cnt, 0) as assignedProjectsCount,
             pn.names as assignedProjectNames,
             tm.default_penalty as defaultPenalty,
             tm.write_only_reviewed as writeOnlyReviewed
      from translation_memory tm
      left join lateral (
        -- For SHARED TMs the entry count is just the stored rows (synced + manual). For PROJECT
        -- TMs we add the virtual-entry count: distinct base-language texts contributed by the
        -- owning project translations under the same write-only-reviewed/default-branch filter
        -- applied in TranslationMemoryEntryManagementService.loadVirtualGroups, so the count
        -- visible on the TMs list matches the row count of the content browser.
        select (
                 select count(distinct e.source_text)
                 from translation_memory_entry e
                 where e.translation_memory_id = tm.id
               )
             + coalesce((
                 select count(distinct base_t.text)
                 from translation_memory_project tmp
                 join project p on p.id = tmp.project_id
                 join language base_lang on base_lang.id = p.base_language_id
                 join key k on k.project_id = p.id
                 left join branch b on b.id = k.branch_id
                 join translation base_t on base_t.key_id = k.id
                                        and base_t.language_id = base_lang.id
                 join translation target_t on target_t.key_id = k.id
                                          and target_t.language_id <> base_lang.id
                 where tm.type = 'PROJECT'
                   and tmp.translation_memory_id = tm.id
                   and base_t.text is not null and base_t.text <> ''
                   and target_t.text is not null and target_t.text <> ''
                   and (b.id is null or b.is_default = true)
                   and (not tm.write_only_reviewed or target_t.state = 2)
               ), 0) as cnt
      ) ec on true
      left join lateral (
        select count(*) as cnt
        from translation_memory_project tp
        where tp.translation_memory_id = tm.id
      ) pc on true
      left join lateral (
        select string_agg(p.name, ',' order by p.name) as names
        from (
          select pr.name
          from translation_memory_project tp2
          join project pr on pr.id = tp2.project_id
          where tp2.translation_memory_id = tm.id
          order by pr.name
          limit 3
        ) p
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
