package io.tolgee.ee.repository.glossary

import io.tolgee.ee.data.glossary.GlossaryWithStats
import io.tolgee.model.Project
import io.tolgee.model.glossary.Glossary
import io.tolgee.repository.ProjectRepository
import org.springframework.context.annotation.Lazy
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
@Lazy
interface GlossaryRepository : JpaRepository<Glossary, Long> {
  companion object {
    /**
     * The canonical [io.tolgee.repository.ProjectRepository.BELOW_MEMBER_ACCESSIBLE_PROJECT]; consuming
     * queries must join its `r` (assigned project) / `bl` (base language) / `o` (org) aliases + `:userId`.
     */
    const val ASSIGNED_PROJECT_BELOW_MEMBER_ACCESSIBLE = ProjectRepository.BELOW_MEMBER_ACCESSIBLE_PROJECT

    /** A glossary a below-member reader may read: has at least one [ASSIGNED_PROJECT_BELOW_MEMBER_ACCESSIBLE] project. Expects alias `g`. */
    const val BELOW_MEMBER_ACCESSIBLE = """
      exists (
        select r.id from Glossary g2
          join g2.assignedProjects r
          left join r.baseLanguage bl
          join r.organizationOwner o
        where g2.id = g.id and $ASSIGNED_PROJECT_BELOW_MEMBER_ACCESSIBLE
      )
    """
  }

  @Query(
    """
    from Glossary
    where organizationOwner.id = :organizationId
      and organizationOwner.deletedAt is null
      and id = :glossaryId
  """,
  )
  fun find(
    organizationId: Long,
    glossaryId: Long,
  ): Glossary?

  @Query(
    """
    select g from Glossary g
    where g.organizationOwner.id = :organizationId
      and g.organizationOwner.deletedAt is null
      and g.id = :glossaryId
      and ($BELOW_MEMBER_ACCESSIBLE)
  """,
  )
  fun findBelowMemberAccessible(
    organizationId: Long,
    glossaryId: Long,
    userId: Long?,
  ): Glossary?

  @Query(
    """
    from Glossary
    where organizationOwner.id = :organizationId
      and organizationOwner.deletedAt is null
  """,
  )
  fun findByOrganizationId(organizationId: Long): List<Glossary>

  @Query(
    """
    from Glossary
    where organizationOwner.id = :organizationId
      and organizationOwner.deletedAt is null
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
    select g from Glossary g
    where g.organizationOwner.id = :organizationId
      and g.organizationOwner.deletedAt is null
      and (lower(g.name) like lower(concat('%', coalesce(:search, ''), '%')) or :search is null)
      and ($BELOW_MEMBER_ACCESSIBLE)
  """,
  )
  fun findByOrganizationIdBelowMemberPaged(
    organizationId: Long,
    userId: Long?,
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
      and (lower(g.name) like lower(concat('%', coalesce(:search, ''), '%')) or :search is null)
    group by g.id
  """,
  )
  fun findByOrganizationIdWithStatsPaged(
    organizationId: Long,
    pageable: Pageable,
    search: String?,
  ): Page<GlossaryWithStats>

  /**
   * Below-member variant: joins only the projects the user can access, so private assigned projects
   * leak neither into `firstAssignedProjectName` nor the count.
   */
  @Query(
    """
    select g.id as id,
      g.name as name,
      g.baseLanguageTag as baseLanguageTag,
      min(r.name) as firstAssignedProjectName,
      count(r) as assignedProjectsCount
    from Glossary g
    join g.assignedProjects r
    left join r.baseLanguage bl
    join r.organizationOwner o
    where g.organizationOwner.id = :organizationId
      and g.organizationOwner.deletedAt is null
      and (lower(g.name) like lower(concat('%', coalesce(:search, ''), '%')) or :search is null)
      and $ASSIGNED_PROJECT_BELOW_MEMBER_ACCESSIBLE
    group by g.id
  """,
  )
  fun findByOrganizationIdWithStatsBelowMemberPaged(
    organizationId: Long,
    userId: Long?,
    pageable: Pageable,
    search: String?,
  ): Page<GlossaryWithStats>

  @Query(
    """
    select gp.project_id
    from glossary_project gp
    where gp.glossary_id = :glossaryId
    """,
    nativeQuery = true,
  )
  fun findAssignedProjectsIdsByGlossaryId(glossaryId: Long): Set<Long>

  @Query(
    """
    select r from Glossary g
      join g.assignedProjects r
      left join r.baseLanguage bl
      join r.organizationOwner o
    where g.id = :glossaryId
      and $ASSIGNED_PROJECT_BELOW_MEMBER_ACCESSIBLE
    order by r.name
    """,
  )
  fun findBelowMemberAccessibleAssignedProjects(
    glossaryId: Long,
    userId: Long?,
  ): List<Project>

  @Query(
    """
    select distinct g
    from Glossary g
      join g.assignedProjects ap
    where g.organizationOwner.id = :organizationId
      and g.organizationOwner.deletedAt is null
      and ap.id = :projectId
    order by g.name
    """,
  )
  fun findAssignedToProject(
    organizationId: Long,
    projectId: Long,
  ): List<Glossary>

  @Query(
    """
    delete from glossary_project gp
    using glossary g
    where gp.glossary_id = :glossaryId
      and gp.project_id = :projectId 
      and gp.glossary_id = g.id
      and g.organization_owner_id = :organizationId
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
    delete from glossary_project gp
    where gp.project_id = :projectId
    """,
    nativeQuery = true,
  )
  @Modifying
  fun unassignProjectFromAll(projectId: Long): Int
}
