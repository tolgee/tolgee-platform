package io.tolgee.repository

import io.tolgee.dtos.queryResults.UserAccountView
import io.tolgee.dtos.request.task.UserAccountFilters
import io.tolgee.model.UserAccount
import io.tolgee.model.views.UserAccountInProjectView
import io.tolgee.model.views.UserAccountWithOrganizationRoleView
import org.springframework.context.annotation.Lazy
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.*

private const val USER_FILTERS = """
    (
        :#{#filters.filterId} is null
        or ua.id in :#{#filters.filterId}
    )
    and (
        :#{#filters.filterNotId} is null
        or ua.id not in :#{#filters.filterNotId}
    )
"""

private const val PROJECT_PERMISSIONS_CTE = """
    with projectPermissions as (
        select
            pe.id,
            pe.user_id,
            pe.scopes,
            pe.project_id,
            pe.type,
            array_remove(array_agg(pe_view.view_languages_id), null)          as view_languages,
            array_remove(array_agg(pe_edit.languages_id), null)               as edit_languages,
            array_remove(array_agg(pe_state.state_change_languages_id), null) as state_languages
        from permission pe
                 left join permission_view_languages pe_view on pe.id = pe_view.permission_id
                 left join permission_languages pe_edit on pe.id = pe_edit.permission_id
                 left join permission_state_change_languages pe_state on pe.id = pe_state.permission_id
        where pe.project_id = :projectId
        group by pe.id, pe.user_id, pe.scopes, pe.project_id, pe.type
    )"""
private const val PROJECT_PERMISSIONS_MAIN = """
    from user_account ua
        left join projectPermissions pe on pe.user_id = ua.id
        left join project p on p.id = :projectId
        left join organization o on p.organization_owner_id = o.id
        left join organization_role o_r on o_r.user_id = ua.id and o_r.organization_id = o.id
        left join permission ope on ope.organization_id = o.id
    where (
        pe.project_id= :projectId
        or o_r.user_id is not null
    ) and (
        :filterId is null
        or ua.id in :filterId
    ) and (
        (:scopes is null and :projectRoles is null)
        or (
            (
                cast(:scopes as character varying[]) && pe.scopes
                or pe.type in :projectRoles
            ) and (
                :viewLanguageId is null or
                cardinality(pe.view_languages) = 0 or
                :viewLanguageId = any(pe.view_languages)
            ) and (
                :editLanguageId is null or
                cardinality(pe.edit_languages) = 0 or
                :editLanguageId = any(pe.edit_languages)
            ) and (
                :stateLanguageId is null or
                cardinality(pe.state_languages) = 0 or
                :stateLanguageId = any(pe.state_languages)
            )
        )
        or (
            pe.id is null 
            and (
              cast(:scopes as character varying[]) && ope.scopes
              or ope.type in :projectRoles
            )
        )
        or o_r.type = 1
    ) and (
        cast(:search as text) is null
        or (
            lower(ua.name) like lower(concat('%', cast(:search as text),'%'))
            or lower(ua.username) like lower(concat('%', cast(:search as text),'%'))
        )
    )
"""

@Repository
@Lazy
interface UserAccountRepository : JpaRepository<UserAccount, Long> {
  fun findByUsername(username: String?): Optional<UserAccount>

  @Query("from UserAccount ua where ua.username = :username and ua.deletedAt is null and ua.disabledAt is null")
  fun findActive(username: String): UserAccount?

  @Query("from UserAccount ua where ua.id = :id and ua.deletedAt is null and ua.disabledAt is null")
  fun findActive(id: Long): UserAccount?

  @Query("from UserAccount ua left join fetch ua.emailVerification where ua.isInitialUser = true")
  fun findInitialUser(): UserAccount?

  @Modifying
  @Query(
    """update UserAccount ua 
    set 
     ua.deletedAt = :now, 
     ua.tokensValidNotBefore = :now,
     ua.password = null, 
     ua.totpKey = null, 
     ua.mfaRecoveryCodes = null,
     ua.thirdPartyAuthId = null,
     ua.thirdPartyAuthType = null,
     ua.avatarHash = null,
     ua.username = 'former',
     ua.name = 'Former user'
     where ua = :user
     """,
  )
  fun softDeleteUser(
    user: UserAccount,
    now: Date,
  )

  @Query(
    """
  select new io.tolgee.dtos.queryResults.UserAccountView(
    ua.id,
    ua.username,
    ua.name,
    case when ev is not null then coalesce(ev.newEmail, ua.username) else null end,
    ua.avatarHash,
    ua.accountType,
    ua.role,
    ua.isInitialUser,
    ua.totpKey
  ) from UserAccount ua
  left join ua.emailVerification ev
  where ua.id = :userAccountId and ua.deletedAt is null and ua.disabledAt is null
  """,
  )
  fun findActiveView(userAccountId: Long): UserAccountView

  @Query(
    """
    from UserAccount ua 
      where ua.thirdPartyAuthId = :thirdPartyAuthId 
        and ua.thirdPartyAuthType = :thirdPartyAuthType
        and ua.deletedAt is null
        and ua.disabledAt is null
  """,
  )
  fun findThirdByThirdParty(
    thirdPartyAuthId: String,
    thirdPartyAuthType: String,
  ): Optional<UserAccount>

  @Query(
    """ select ua.id as id, ua.name as name, ua.username as username, mr.type as organizationRole,
          ua.avatarHash as avatarHash
        from UserAccount ua 
        left join ua.organizationRoles mr on mr.organization.id = :organizationId
        left join ua.permissions pp
        left join pp.project p on p.deletedAt is null
        left join p.organizationOwner o on o.id = :organizationId
        where (o is not null or mr is not null) and ((lower(ua.name)
        like lower(concat('%', cast(:search as text),'%')) 
        or lower(ua.username) like lower(concat('%', cast(:search as text),'%'))) or cast(:search as text) is null)
        and ua.deletedAt is null
        group by ua.id, mr.type
        """,
  )
  fun getAllInOrganization(
    organizationId: Long,
    pageable: Pageable,
    search: String,
  ): Page<UserAccountWithOrganizationRoleView>

  @Query(
    """
        select ua.id as id, ua.name as name, ua.username as username, p as directPermission,
          orl.type as organizationRole, ua.avatarHash as avatarHash 
        from UserAccount ua
        left join Project r on r.id = :projectId
        left join ua.permissions p on p.project.id = :projectId
        left join ua.organizationRoles orl on orl.organization = r.organizationOwner
        where r.id = :projectId and (p is not null or orl is not null)
        and (:exceptUserId is null or ua.id <> :exceptUserId)
        and ((lower(ua.name)
        like lower(concat('%', cast(:search as text),'%'))
        or lower(ua.username) like lower(concat('%', cast(:search as text),'%'))) or cast(:search as text) is null)
        and ua.deletedAt is null
        and $USER_FILTERS
    """,
  )
  fun getAllInProject(
    projectId: Long,
    pageable: Pageable,
    search: String? = "",
    exceptUserId: Long? = null,
    filters: UserAccountFilters,
  ): Page<UserAccountInProjectView>

  @Query(
    """
    select ua
    from UserAccount ua
    left join ua.organizationRoles orl
    where orl is null
      and ua.deletedAt is null
  """,
  )
  fun findAllWithoutAnyOrganization(pageable: Pageable): Page<UserAccount>

  @Query(
    """
    select ua.id
    from UserAccount ua
    left join ua.organizationRoles orl
    where orl is null
      and ua.deletedAt is null
  """,
  )
  fun findAllWithoutAnyOrganizationIds(): List<Long>

  @Query(
    """
    from UserAccount userAccount
    where ((lower(userAccount.name)
      like lower(concat('%', cast(:search as text),'%')) 
      or lower(userAccount.username) like lower(concat('%', cast(:search as text),'%'))) or cast(:search as text) is null)
      and userAccount.deletedAt is null
  """,
  )
  fun findAllWithDisabledPaged(
    search: String?,
    pageable: Pageable,
  ): Page<UserAccount>

  @Query(
    value = """
    select ua from UserAccount ua where id in (:ids)
  """,
  )
  fun getAllByIdsIncludingDeleted(ids: Set<Long>): MutableList<UserAccount>

  @Query(
    value = """
    select count(ua) from UserAccount ua where ua.disabledAt is null and ua.deletedAt is null and ua.isDemo = false
  """,
  )
  fun countAllEnabled(): Long

  @Query(
    value = """
    select ua from UserAccount ua where ua.id = :id and ua.disabledAt is not null
  """,
  )
  fun findDisabled(id: Long): UserAccount

  @Query(
    """
    from UserAccount ua
    left join fetch ua.emailVerification
    left join fetch ua.permissions
    where ua.id = :id
  """,
  )
  fun findWithFetchedEmailVerificationAndPermissions(id: Long): UserAccount?

  @Query(
    """
    from UserAccount ua 
    left join fetch ua.emailVerification
    left join fetch ua.permissions
    where ua.username in :usernames and ua.deletedAt is null
  """,
  )
  fun findActiveWithFetchedDataByUserNames(usernames: List<String>): List<UserAccount>

  @Query(
    """
    from UserAccount ua 
    left join fetch ua.organizationRoles
    where ua.username in :usernames and ua.deletedAt is null and ua.isDemo = true
  """,
  )
  fun findDemoByUsernames(usernames: List<String>): List<UserAccount>

  @Query(
    nativeQuery = true,
    value = PROJECT_PERMISSIONS_CTE + "select ua.id" + PROJECT_PERMISSIONS_MAIN,
    countQuery = PROJECT_PERMISSIONS_CTE + "select count(ua.id)" + PROJECT_PERMISSIONS_MAIN,
  )
  fun findUsersWithMinimalPermissions(
    filterId: Collection<Long>,
    scopes: String?,
    projectRoles: Collection<String>,
    projectId: Long,
    viewLanguageId: Long?,
    editLanguageId: Long?,
    stateLanguageId: Long?,
    search: String?,
    pageable: Pageable,
  ): Page<Long>
}
