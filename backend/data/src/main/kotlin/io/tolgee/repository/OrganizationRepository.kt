package io.tolgee.repository

import io.tolgee.dtos.queryResults.organization.OrganizationView
import io.tolgee.model.Organization
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.OrganizationRoleType
import org.springframework.context.annotation.Lazy
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
@Lazy
interface OrganizationRepository : JpaRepository<Organization, Long> {
  @Query(
    """
    from Organization o 
    left join fetch o.basePermission as bp
    where o.slug = :slug and o.deletedAt is null
  """,
  )
  fun findBySlug(slug: String): Organization?

  @Query(
    """select distinct new io.tolgee.dtos.queryResults.organization.OrganizationView(
          o.id,
          o.name,
          o.slug,
          o.description,
          bp._scopes,
          bp.type,
          r.type,
          o.avatarHash
        )
        from Organization o 
        join o.basePermission bp
        left join OrganizationRole r on r.user.id = :userId
          and r.organization = o and (r.type = :roleType or :roleType is null)
        left join o.projects p on p.deletedAt is null
        left join p.permissions perm on perm.user.id = :userId
        where (perm is not null or r is not null)
        and (:search is null or (lower(o.name) like lower(concat('%', cast(:search as text), '%'))))
        and (:exceptOrganizationId is null or (o.id <> :exceptOrganizationId)) and o.deletedAt is null
        """,
    countQuery =
      """select count(o)
        from Organization o 
        left join OrganizationRole r on r.user.id = :userId
          and r.organization = o and (r.type = :roleType or :roleType is null)
        left join o.projects p on p.deletedAt is null
        left join p.permissions perm on perm.user.id = :userId
        where (perm is not null or r is not null)
        and (:search is null or (lower(o.name) like lower(concat('%', cast(:search as text), '%'))))
        and (:exceptOrganizationId is null or (o.id <> :exceptOrganizationId)) and o.deletedAt is null
        """,
  )
  fun findAllPermitted(
    userId: Long?,
    pageable: Pageable,
    roleType: OrganizationRoleType? = null,
    search: String? = null,
    exceptOrganizationId: Long? = null,
  ): Page<OrganizationView>

  @Query(
    """
    select o
    from Organization o
    left join fetch o.basePermission bp
    left join o.memberRoles mr on mr.user.id = :userId
    left join o.projects p on p.deletedAt is null
    left join p.permissions perm on perm.user.id = :userId
    where (perm is not null or mr is not null) and o.id <> :exceptOrganizationId and o.deletedAt is null
    group by mr.id, o.id, bp.id
    order by mr.id asc nulls last
  """,
  )
  fun findPreferred(
    userId: Long,
    exceptOrganizationId: Long,
    pageable: Pageable,
  ): Page<Organization>

  @Query(
    """
    select count(o) > 0
    from Organization o
    left join o.memberRoles mr on mr.user.id = :userId
    left join o.projects p on p.deletedAt is null
    left join p.permissions perm on perm.user.id = :userId
    where (perm is not null or mr is not null) and o.id = :organizationId and o.deletedAt is null
  """,
  )
  fun canUserView(
    userId: Long,
    organizationId: Long,
  ): Boolean

  @Query(
    """
    select count(o) > 0
    from Organization o
    where o.slug = :slug
  """,
  )
  fun organizationWithSlugExists(slug: String): Boolean

  @Query(
    """
    from Organization o
    where o.name = :name and o.deletedAt is null
  """,
  )
  fun findAllByName(name: String): List<Organization>

  @Query(
    """
    from Organization o 
    join o.memberRoles mr on mr.user = :user
    join mr.user u
    where o.name = u.name and mr.type = 1 and o.deletedAt is null
  """,
  )
  fun findUsersDefaultOrganization(user: UserAccount): Organization?

  @Query(
    """select distinct new io.tolgee.dtos.queryResults.organization.OrganizationView(
          o.id,
          o.name,
          o.slug,
          o.description,
          bp._scopes,
          bp.type,
          r.type,
          o.avatarHash
        )
        from Organization o
        join o.basePermission bp
        left join OrganizationRole r on r.user.id = :userId and r.organization = o
        where (
              lower(o.name) like lower(concat('%', coalesce(:search, ''), '%')) or
              lower(o.slug) like lower(concat('%', coalesce(:search, ''), '%')) or
              lower(cast(o.id as string)) like lower(concat(coalesce(:search, ''), '%')) or
              :search is null
              )
        and o.deletedAt is null
        """,
    countQuery =
      """select count(o)
        from Organization o
        where (
              lower(o.name) like lower(concat('%', coalesce(:search, ''), '%')) or
              lower(o.slug) like lower(concat('%', coalesce(:search, ''), '%')) or
              lower(cast(o.id as string)) like lower(concat(coalesce(:search, ''), '%')) or
              :search is null
              )
        and o.deletedAt is null
        """,
  )
  fun findAllViews(
    pageable: Pageable,
    search: String?,
    userId: Long,
  ): Page<OrganizationView>

  @Query(
    """select o
    from Organization o 
    join o.memberRoles mr on mr.user = :userAccount and mr.type = :type
    join o.memberRoles mra on mra.type = :type
    where o.deletedAt is null
    group by o.id, mr.id
    having count(mra.id) = 1
 """,
  )
  fun getAllSingleOwnedByUser(
    userAccount: UserAccount,
    type: OrganizationRoleType = OrganizationRoleType.OWNER,
  ): List<Organization>

  @Query(
    """
    from Organization o 
    join o.projects p on p.id = :projectId and p.deletedAt is null
    join fetch o.basePermission
    where o.deletedAt is null
  """,
  )
  fun getProjectOwner(projectId: Long): Organization

  @Query(
    """
    from Organization o 
    left join fetch o.basePermission
    left join fetch o.mtCreditBucket
    where o = :organization
  """,
  )
  fun fetchData(organization: Organization): Organization

  @Query(
    """
    from Organization o
    where o.id = :id and o.deletedAt is null
  """,
  )
  fun find(id: Long): Organization?

  @Query(
    """
    select new io.tolgee.dtos.queryResults.organization.OrganizationView(
      o.id,
      o.name,
      o.slug,
      o.description,
      bp._scopes,
      bp.type,
      r.type,
      o.avatarHash
    )
    from Organization o
    join o.basePermission bp
    left join o.memberRoles r on r.user.id = :currentUserId 
    where o.id = :id and o.deletedAt is null
  """,
  )
  fun findView(
    id: Long,
    currentUserId: Long,
  ): OrganizationView?

  /**
   * Returns all organizations where user is counted as seat
   * For translation agencies, we don't count them as seats
   */
  @Query(
    """
      select distinct o.id from Organization o
        left join o.memberRoles orl
        left join o.projects pr
        left join pr.permissions perm on perm.agency is null
        where orl.user.id = :userId or perm.user.id = :userId
  """,
  )
  fun getAllUsersOrganizationsToCountUsageFor(userId: Long): Set<Long>

  @Query(
    """
      select ua.id $ALL_USERS_IN_ORGANIZATION_QUERY_TO_COUNT_USAGE_FOR
    """,
  )
  fun getAllUserIdsInOrganizationToCountSeats(organizationId: Long): Set<Long>

  companion object {
    /**
     * Query to count all users in organization to count seats
     */
    const val ALL_USERS_IN_ORGANIZATION_QUERY_TO_COUNT_USAGE_FOR = """from UserAccount ua
      left join ua.organizationRoles orl
      left join orl.organization o on o.deletedAt is null and o.id = :organizationId
      left join ua.permissions p on p.agency is null
      left join p.project pr on pr.deletedAt is null and pr.organizationOwner.id = :organizationId 
      where ua.deletedAt is null and ua.disabledAt is null
      and (pr is not null or o is not null)"""
  }
}
