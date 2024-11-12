package io.tolgee.repository

import io.tolgee.dtos.request.project.ProjectFilters
import io.tolgee.model.Organization
import io.tolgee.model.Project
import io.tolgee.model.views.ProjectView
import org.springframework.context.annotation.Lazy
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
@Lazy
interface ProjectRepository : JpaRepository<Project, Long> {
  companion object {
    const val BASE_VIEW_QUERY = """select r.id as id, r.name as name, r.description as description,
        r.slug as slug, r.avatarHash as avatarHash,
        dn as defaultNamespace, o as organizationOwner,
        role.type as organizationRole, p as directPermission, r.icuPlaceholders as icuPlaceholders
        from Project r
        left join r.baseLanguage bl
        left join r.defaultNamespace dn
        left join Permission p on p.project = r and p.user.id = :userAccountId
        left join Organization o on r.organizationOwner = o
        left join fetch o.basePermission
        left join OrganizationRole role on role.organization = o and role.user.id = :userAccountId
        """

    const val FILTERS = """
        (
            :#{#filters.filterId} is null
            or r.id in :#{#filters.filterId}
        )
        and (
            :#{#filters.filterNotId} is null
            or r.id not in :#{#filters.filterNotId}
        )
    """
  }

  @Query(
    """select r, p, o, role from Project r 
        left join fetch Permission p on p.project = r and p.user.id = :userAccountId
        left join fetch Organization o on r.organizationOwner = o
        left join fetch OrganizationRole role on role.organization = o and role.user.id = :userAccountId
        where (p is not null or (role is not null)) and r.deletedAt is null 
        order by r.name
        """,
  )
  fun findAllPermitted(userAccountId: Long): List<Array<Any>>

  // it makes sense to give all projects to admin only when organizationId is provided
  @Query(
    """$BASE_VIEW_QUERY        
        left join UserAccount ua on ua.id = :userAccountId
        left join o.basePermission
        where (
            (p is not null and (p.type <> 'NONE' or p.type is null)) or 
            (role is not null and (o.basePermission.type <> 'NONE' or o.basePermission.type is null) and p is null) or
            (ua.role = 'ADMIN' and :organizationId is not null))
        and (
            :search is null or (lower(r.name) like lower(concat('%', cast(:search as text), '%'))
            or lower(o.name) like lower(concat('%', cast(:search as text),'%')))
        )
        and (:organizationId is null or o.id = :organizationId) and r.deletedAt is null
        and (
            :#{#filters.filterId} is null
            or r.id in :#{#filters.filterId}
        )
        and (
            :#{#filters.filterNotId} is null
            or r.id not in :#{#filters.filterNotId}
        )
    """,
  )
  fun findAllPermitted(
    userAccountId: Long,
    pageable: Pageable,
    @Param("search") search: String? = null,
    organizationId: Long? = null,
    filters: ProjectFilters,
  ): Page<ProjectView>

  fun findAllByOrganizationOwnerId(organizationOwnerId: Long): List<Project>

  fun countAllBySlug(slug: String): Long

  @Suppress("SpringDataRepositoryMethodReturnTypeInspection")
  @Query(
    """
    $BASE_VIEW_QUERY
    where r.id = :projectId and r.deletedAt is null
  """,
  )
  fun findViewById(
    userAccountId: Long,
    projectId: Long,
  ): ProjectView?

  @Query(
    """
      from Project p where p.name = :name and p.deletedAt is null
    """,
  )
  fun findAllByName(name: String): List<Project>

  @Query(
    """
    from Project p 
    where p.name = :name and p.organizationOwner = :organization and p.deletedAt is null
  """,
  )
  fun findAllByNameAndOrganizationOwner(
    name: String,
    organization: Organization,
  ): List<Project>

  @Query(
    """
    from Project p 
    where p.organizationOwner is null and p.userOwner is not null and p.deletedAt is null
  """,
  )
  fun findAllWithUserOwner(pageable: Pageable): Page<Project>

  @Query(
    """
    select p.id
    from Project p
    where p.organizationOwner is null and p.deletedAt is null
  """,
  )
  fun findAllWithUserOwnerIds(): List<Long>

  @Query(
    """
    select pp.user.id, p 
    from Project p
    join p.permissions pp on pp.user.id in :userIds
    join fetch p.baseLanguage
    where p.organizationOwner.id = :organizationId and p.deletedAt is null
  """,
  )
  fun getProjectsWithDirectPermissions(
    organizationId: Long,
    userIds: List<Long>,
  ): List<Array<Any>>

  @Query(
    """
    from Project where id = :id and deletedAt is null
  """,
  )
  fun find(id: Long): Project?
}
