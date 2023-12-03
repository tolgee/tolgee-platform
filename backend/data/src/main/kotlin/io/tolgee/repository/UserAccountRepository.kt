package io.tolgee.repository

import io.tolgee.model.UserAccount
import io.tolgee.model.views.UserAccountInProjectView
import io.tolgee.model.views.UserAccountProjectPermissionsNotificationPreferencesDataView
import io.tolgee.model.views.UserAccountWithOrganizationRoleView
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface UserAccountRepository : JpaRepository<UserAccount, Long> {
  fun findByUsername(username: String?): Optional<UserAccount>

  @Query("from UserAccount ua where ua.username = :username and ua.deletedAt is null and ua.disabledAt is null")
  fun findActive(username: String): UserAccount?

  @Query("from UserAccount ua where ua.id = :id and ua.deletedAt is null and ua.disabledAt is null")
  fun findActive(id: Long): UserAccount?

  @Query("from UserAccount ua where ua.isInitialUser = true")
  fun findInitialUser(): UserAccount?

  @Modifying
  @Query(
    """update UserAccount ua 
    set 
     ua.deletedAt = now(), 
     ua.tokensValidNotBefore = now(),
     ua.password = null, 
     ua.totpKey = null, 
     ua.mfaRecoveryCodes = null,
     ua.thirdPartyAuthId = null,
     ua.thirdPartyAuthType = null,
     ua.avatarHash = null,
     ua.username = 'former',
     ua.name = 'Former user'
     where ua = :user
     """
  )
  fun softDeleteUser(user: UserAccount)

  @Query(
    """
    from UserAccount ua 
      where ua.thirdPartyAuthId = :thirdPartyAuthId 
        and ua.thirdPartyAuthType = :thirdPartyAuthType
        and ua.deletedAt is null
        and ua.disabledAt is null
  """
  )
  fun findThirdByThirdParty(
    thirdPartyAuthId: String,
    thirdPartyAuthType: String
  ): Optional<UserAccount>

  @Query(
    """ select ua.id as id, ua.name as name, ua.username as username, mr.type as organizationRole,
          ua.avatarHash as avatarHash
        from UserAccount ua 
        left join ua.organizationRoles mr on mr.organization.id = :organizationId
        left join ua.permissions pp
        left join pp.project p
        left join p.organizationOwner o on o.id = :organizationId
        where (o is not null or mr is not null) and ((lower(ua.name)
        like lower(concat('%', cast(:search as text),'%')) 
        or lower(ua.username) like lower(concat('%', cast(:search as text),'%'))) or cast(:search as text) is null)
        and ua.deletedAt is null
        group by ua.id, mr.type
        """
  )
  fun getAllInOrganization(
    organizationId: Long,
    pageable: Pageable,
    search: String
  ): Page<UserAccountWithOrganizationRoleView>

  @Query(
    """
        select ua.id as id, ua.name as name, ua.username as username, p as directPermission,
          orl.type as organizationRole, ua.avatarHash as avatarHash 
        from UserAccount ua, Project r 
        left join fetch Permission p on ua = p.user and p.project.id = :projectId
        left join OrganizationRole orl on orl.user = ua and r.organizationOwner = orl.organization
        where r.id = :projectId and (p is not null or orl is not null)
        and (:exceptUserId is null or ua.id <> :exceptUserId)
        and ((lower(ua.name)
        like lower(concat('%', cast(:search as text),'%'))
        or lower(ua.username) like lower(concat('%', cast(:search as text),'%'))) or cast(:search as text) is null)
        and ua.deletedAt is null
    """
  )
  fun getAllInProject(
    projectId: Long,
    pageable: Pageable,
    search: String? = "",
    exceptUserId: Long? = null
  ): Page<UserAccountInProjectView>

  @Query(
    """
    select ua
    from UserAccount ua
    left join ua.organizationRoles orl
    where orl is null
      and ua.deletedAt is null
  """
  )
  fun findAllWithoutAnyOrganization(pageable: Pageable): Page<UserAccount>

  @Query(
    """
    select ua.id
    from UserAccount ua
    left join ua.organizationRoles orl
    where orl is null
      and ua.deletedAt is null
  """
  )
  fun findAllWithoutAnyOrganizationIds(): List<Long>

  @Query(
    """
    from UserAccount userAccount
    where ((lower(userAccount.name)
      like lower(concat('%', cast(:search as text),'%')) 
      or lower(userAccount.username) like lower(concat('%', cast(:search as text),'%'))) or cast(:search as text) is null)
      and userAccount.deletedAt is null
  """
  )
  fun findAllWithDisabledPaged(search: String?, pageable: Pageable): Page<UserAccount>

  @Query(
    value = """
    select ua from UserAccount ua where ua.id in (:ids)
  """
  )
  fun getAllByIdsIncludingDeleted(ids: Set<Long>): MutableList<UserAccount>

  @Query(
    value = """
    select count(ua) from UserAccount ua where ua.disabledAt is null and ua.deletedAt is null
  """
  )
  fun countAllEnabled(): Long

  @Query(
    value = """
    select ua from UserAccount ua where ua.id = :id and ua.disabledAt is not null
  """
  )
  fun findDisabled(id: Long): UserAccount

  @Query(
    """
      SELECT new io.tolgee.model.views.UserAccountProjectPermissionsNotificationPreferencesDataView(
        ua.id,
        p.id,
        org_r.type,
        perm_org.type,
        perm_org._scopes,
        perm.type,
        perm._scopes,
        array_to_string(array_agg(l.id), ','),
        np_global,
        np_project
      )
      FROM UserAccount ua, Project p
      LEFT JOIN OrganizationRole org_r ON
        org_r.user = ua AND
        org_r.organization = p.organizationOwner
      LEFT JOIN Permission perm ON
        perm.user = ua AND
        perm.project = p
      LEFT JOIN Permission perm_org ON
        org_r.user = ua AND
        org_r.organization = p.organizationOwner AND
        perm_org.organization = p.organizationOwner
      LEFT JOIN Language l ON l IN elements(perm.viewLanguages)
      LEFT JOIN FETCH NotificationPreferences np_global ON np_global.userAccount = ua AND np_global.project IS NULL
      LEFT JOIN FETCH NotificationPreferences np_project ON np_project.userAccount = ua AND np_project.project = p
      WHERE
        p.id = :projectId AND
        ua.deletedAt IS NULL AND (
          (perm._scopes IS NOT NULL AND perm._scopes != '{}') OR perm.type IS NOT NULL OR
          (perm_org._scopes IS NOT NULL AND perm_org._scopes != '{}') OR perm_org.type IS NOT NULL
        )
      GROUP BY ua.id, p.id, org_r.type, perm_org.type, perm_org._scopes, perm.type, perm._scopes, np_global, np_project
    """
  )
  fun findAllPermittedUsersProjectPermissionNotificationPreferencesView(projectId: Long):
    List<UserAccountProjectPermissionsNotificationPreferencesDataView>
}
