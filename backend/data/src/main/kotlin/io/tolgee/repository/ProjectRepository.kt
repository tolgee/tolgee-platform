package io.tolgee.repository

import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.model.views.ProjectView
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface ProjectRepository : JpaRepository<Project, Long> {
  companion object {
    const val BASE_VIEW_QUERY = """select r.id as id, r.name as name, r.description as description,
        r.slug as slug, r.avatarHash as avatarHash,
        ua as userOwner, o.name as organizationOwnerName, o.slug as organizationOwnerSlug, 
        bl as baseLanguage,
        o.basePermissions as organizationBasePermissions, role.type as organizationRole, p.type as directPermissions
        from Project r
        left join r.baseLanguage bl
        left join UserAccount ua on ua.id = r.userOwner.id
        left join Permission p on p.project = r and p.user.id = :userAccountId
        left join Organization o on r.organizationOwner = o
        left join OrganizationRole role on role.organization = o and role.user.id = :userAccountId
        where (p is not null or role is not null)
        """
  }

  @Query(
    """from Project r 
        left join fetch Permission p on p.project = r and p.user.id = :userAccountId
        left join fetch Organization o on r.organizationOwner = o
        left join fetch OrganizationRole role on role.organization = o and role.user.id = :userAccountId
        where p is not null or (role is not null)
        order by r.name
        """
  )
  fun findAllPermitted(userAccountId: Long): List<Array<Any>>

  @Query(
    """$BASE_VIEW_QUERY 
        and (:search is null or (lower(r.name) like lower(concat('%', cast(:search as text), '%'))
        or lower(o.name) like lower(concat('%', cast(:search as text),'%')))
        or lower(ua.name) like lower(concat('%', cast(:search as text),'%')))
    """
  )
  fun findAllPermitted(
    userAccountId: Long,
    pageable: Pageable,
    @Param("search") search: String? = null
  ): Page<ProjectView>

  fun findAllByOrganizationOwnerId(organizationOwnerId: Long): List<io.tolgee.model.Project>

  @Query(
    """
      $BASE_VIEW_QUERY and o.id = :organizationOwnerId and o is not null
      and ((lower(r.name) like lower(concat('%', cast(:search as text),'%'))
      or lower(o.name) like lower(concat('%', cast(:search as text),'%')))
      or lower(ua.name) like lower(concat('%', cast(:search as text),'%')) or cast(:search as text) is null)
        """
  )
  fun findAllPermittedInOrganization(
    userAccountId: Long,
    organizationOwnerId: Long,
    pageable: Pageable,
    search: String?
  ): Page<ProjectView>

  fun countAllBySlug(slug: String): Long

  @Suppress("SpringDataRepositoryMethodReturnTypeInspection")
  @Query(
    """
    $BASE_VIEW_QUERY and r.id = :projectId
  """
  )
  fun findViewById(userAccountId: Long, projectId: Long): ProjectView?

  fun findAllByName(name: String): List<Project>

  @Query(
    """
      from Project p 
      left join fetch p.languages
      where p.id in :ids
    """
  )
  fun findAllWithLanguages(ids: Iterable<Long>): List<Project>

  @Query(
    """
      from Project p left join fetch p.languages left join fetch p.baseLanguage where p.id in :projectIds
    """
  )
  fun getWithLanguages(projectIds: Iterable<Long>): List<Project>

  fun findAllByNameAndUserOwner(name: String, userOwner: UserAccount): List<Project>
}
