package io.tolgee.repository

import io.tolgee.model.Organization
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.OrganizationRoleType
import io.tolgee.model.views.OrganizationView
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface OrganizationRepository : JpaRepository<Organization, Long> {
  fun getOneBySlug(slug: String): Organization?

  @Query(
    """select distinct o.id as id, o.name as name, o.description as description, o.slug as slug,
        bp as basePermission, r.type as currentUserRole, o.avatarHash as avatarHash
        from Organization o 
        left join OrganizationRole r on r.user.id = :userId
        and r.organization = o and (r.type = :roleType or :roleType is null)
        left join o.projects p
        join o.basePermission bp
        left join p.permissions perm on perm.user.id = :userId
        where (perm is not null or r is not null)
        and (:search is null or (lower(o.name) like lower(concat('%', cast(:search as text), '%'))))
        and (:exceptOrganizationId is null or (o.id <> :exceptOrganizationId))
        """
  )
  fun findAllPermitted(
    userId: Long?,
    pageable: Pageable,
    roleType: OrganizationRoleType? = null,
    search: String? = null,
    exceptOrganizationId: Long? = null
  ): Page<OrganizationView>

  @Query(
    """
    select o
    from Organization o
    left join o.memberRoles mr on mr.user.id = :userId
    left join o.projects p
    left join p.permissions perm on perm.user.id = :userId
    where (perm is not null or mr is not null) and o.id <> :exceptOrganizationId
    group by mr.id, o.id
    order by mr.id asc nulls last
  """
  )
  fun findPreferred(
    userId: Long,
    exceptOrganizationId: Long,
    pageable: Pageable
  ): Page<Organization>

  @Query(
    """
    select count(o) > 0
    from Organization o
    left join o.memberRoles mr on mr.user.id = :userId
    left join o.projects p
    left join p.permissions perm on perm.user.id = :userId
    join UserAccount ua on perm.user = ua or mr.user = ua
    where ((perm is not null or mr is not null) or ua.role = 'ADMIN')  and o.id = :organizationId
  """
  )
  fun canUserView(userId: Long, organizationId: Long): Boolean

  fun countAllBySlug(slug: String): Long
  fun findAllByName(name: String): List<Organization>

  @Query(
    """
    from Organization o 
    join o.memberRoles mr on mr.user = :user
    join mr.user u
    where o.name = u.name and mr.type = 1
  """
  )
  fun findUsersDefaultOrganization(user: UserAccount): Organization?

  @Query(
    """select distinct o.id as id, o.name as name, o.description as description, o.slug as slug,
        o.basePermissions as basePermissions, r.type as currentUserRole, o.avatarHash as avatarHash
        from Organization o 
        left join OrganizationRole r on r.user.id = :userId and r.organization = o
        left join o.projects p
        left join p.permissions perm on perm.user.id = :userId
        where (:search is null or (lower(o.name) like lower(concat('%', cast(:search as text), '%'))))
        """
  )
  fun findAllViews(pageable: Pageable, search: String?, userId: Long): Page<OrganizationView>

  @Query(
    """select o
    from Organization o 
    join o.memberRoles mr on mr.user = :userAccount and mr.type = :type
    join o.memberRoles mra on mra.type = :type
    group by o.id, mr.id
    having count(mra.id) = 1
 """
  )
  fun getAllSingleOwnedByUser(
    userAccount: UserAccount,
    type: OrganizationRoleType = OrganizationRoleType.OWNER
  ): List<Organization>
}
