package io.tolgee.ee.repository.glossary

import io.tolgee.ee.data.glossary.GlossaryWithStats
import io.tolgee.model.glossary.Glossary
import org.springframework.context.annotation.Lazy
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.Date

@Repository
@Lazy
interface GlossaryRepository : JpaRepository<Glossary, Long> {
  @Query(
    """
    from Glossary
    where organizationOwner.id = :organizationId
      and organizationOwner.deletedAt is null
      and id = :glossaryId
      and deletedAt is null
  """,
  )
  fun find(
    organizationId: Long,
    glossaryId: Long,
  ): Glossary?

  @Query(
    """
    from Glossary
    where organizationOwner.id = :organizationId
      and organizationOwner.deletedAt is null
      and deletedAt is null
  """,
  )
  fun findByOrganizationId(organizationId: Long): List<Glossary>

  @Query(
    """
    from Glossary
    where organizationOwner.id = :organizationId
      and organizationOwner.deletedAt is null
      and deletedAt is null
      and (lower(name) like lower(concat('%', coalesce(:search, ''), '%')) or :search is null)
  """,
  )
  fun findByOrganizationIdPaged(
    organizationId: Long,
    pageable: Pageable,
    search: String?,
  ): Page<Glossary>

  @Query(
    """
    select g.id as id,
      g.name as name,
      g.baseLanguageTag as baseLanguageTag,
      min(ap.name) as firstAssignedProjectName,
      count(ap) as assignedProjectsCount
    from Glossary g
    left join g.assignedProjects ap
    where g.organizationOwner.id = :organizationId
      and g.organizationOwner.deletedAt is null
      and g.deletedAt is null
      and (lower(g.name) like lower(concat('%', coalesce(:search, ''), '%')) or :search is null)
    group by g.id
  """,
  )
  fun findByOrganizationIdWithStatsPaged(
    organizationId: Long,
    pageable: Pageable,
    search: String?,
  ): Page<GlossaryWithStats>

  @Query(
    """
    delete from glossary_project gp
    using glossary g
    where gp.glossary_id = :glossaryId
      and gp.project_id = :projectId 
      and gp.glossary_id = g.id
      and g.organization_owner_id = :organizationId
      and g.deleted_at is null
    """,
    nativeQuery = true,
  )
  @Modifying
  fun unassignProject(
    organizationId: Long,
    glossaryId: Long,
    projectId: Long,
  ): Int

  @Query(
    """
    update Glossary
    set deletedAt = :deletedAt
    where organizationOwner.id = :organizationId
      and id = :glossaryId
      and deletedAt is null
    """,
  )
  @Modifying
  fun softDelete(
    organizationId: Long,
    glossaryId: Long,
    deletedAt: Date,
  ): Int
}
