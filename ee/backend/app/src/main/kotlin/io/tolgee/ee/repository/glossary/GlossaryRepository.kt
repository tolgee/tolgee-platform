package io.tolgee.ee.repository.glossary

import io.tolgee.ee.data.glossary.GlossaryWithStats
import io.tolgee.model.Project
import io.tolgee.model.glossary.Glossary
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
     * An assigned project a below-member reader may see: publicly visible (the project half of
     * [io.tolgee.repository.ProjectRepository.PUBLIC_PROJECT_VISIBILITY]) or one the user holds
     * any permission row on. Expects aliases `ap` (assigned project), `apbl` (its base language)
     * and a `:userId` parameter (nullable — unauthenticated readers match only public projects).
     */
    const val ASSIGNED_PROJECT_BELOW_MEMBER_ACCESSIBLE = """
      ap.deletedAt is null
      and (
        (ap.public = true and ap.baseLanguage is not null and apbl.deletedAt is null)
        or exists (select uperm.id from Permission uperm where uperm.user.id = :userId and uperm.project = ap)
      )
    """

    /** A glossary a below-member reader may read: has at least one [ASSIGNED_PROJECT_BELOW_MEMBER_ACCESSIBLE] project. Expects alias `g`. */
    const val BELOW_MEMBER_ACCESSIBLE = """
      exists (
        select ap.id from Glossary g2
          join g2.assignedProjects ap
          left join ap.baseLanguage apbl
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
      min(ap.name) as firstAssignedProjectName,
      count(ap) as assignedProjectsCount
    from Glossary g
    join g.assignedProjects ap
    left join ap.baseLanguage apbl
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
    select ap from Glossary g
      join g.assignedProjects ap
      left join ap.baseLanguage apbl
    where g.id = :glossaryId
      and $ASSIGNED_PROJECT_BELOW_MEMBER_ACCESSIBLE
    order by ap.name
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
