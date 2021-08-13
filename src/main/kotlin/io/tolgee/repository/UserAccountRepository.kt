package io.tolgee.repository

import io.tolgee.model.UserAccount
import io.tolgee.model.views.UserAccountInProjectView
import io.tolgee.model.views.UserAccountWithOrganizationRoleView
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface UserAccountRepository : JpaRepository<UserAccount, Long> {
  fun findByUsername(username: String?): Optional<UserAccount>
  fun findByThirdPartyAuthTypeAndThirdPartyAuthId(
    thirdPartyAuthId: String,
    thirdPartyAuthType: String
  ): Optional<UserAccount>

  @Query(
    """select userAccount.id as id, userAccount.name as name, userAccount.username as username, memberRole.type as organizationRole from UserAccount userAccount 
        join OrganizationRole memberRole on memberRole.user = userAccount and memberRole.organization.id = :organizationId
        where ((lower(userAccount.name)
        like lower(concat('%', cast(:search as text),'%')) 
        or lower(userAccount.username) like lower(concat('%', cast(:search as text),'%'))) or cast(:search as text) is null)
        """
  )
  fun getAllInOrganization(
    organizationId: Long,
    pageable: Pageable,
    search: String
  ): Page<UserAccountWithOrganizationRoleView>

  @Query(
    """
        select ua.id as id, ua.name as name, ua.username as username, p.type as directPermissions, orl.type as organizationRole,
        orl.organization.id as oid, o.basePermissions as organizationBasePermissions from UserAccount ua, Project r 
        left join Permission p on ua = p.user and p.project.id = :projectId
        left join OrganizationRole orl on orl.user = ua and r.organizationOwner = orl.organization
        left join Organization  o on orl.organization = o
        where r.id = :projectId and (p is not null or orl is not null)
        and ( :exceptUserId is null or ua.id <> :exceptUserId)
        and ((lower(ua.name)
        like lower(concat('%', cast(:search as text),'%'))
        or lower(ua.username) like lower(concat('%', cast(:search as text),'%'))) or cast(:search as text) is null)
    """
  )
  fun getAllInProject(
    projectId: Long,
    pageable: Pageable,
    search: String? = "",
    exceptUserId: Long? = null
  ): Page<UserAccountInProjectView>
}
