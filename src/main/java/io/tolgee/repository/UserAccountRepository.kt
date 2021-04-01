package io.tolgee.repository

import io.tolgee.model.UserAccount
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface UserAccountRepository : JpaRepository<UserAccount?, Long?> {
    fun findByUsername(username: String?): Optional<UserAccount>
    fun findByThirdPartyAuthTypeAndThirdPartyAuthId(thirdPartyAuthId: String, thirdPartyAuthType: String): Optional<UserAccount>

    @Query("""from UserAccount userAccount 
        join fetch OrganizationRole memberRole on memberRole.user = userAccount and memberRole.organization.id = :organizationId
        where (lower(userAccount.name)
        like lower(concat('%', :search,'%')) 
        or lower(userAccount.username) like lower(concat('%', :search,'%')))
        """)
    fun getAllInOrganization(organizationId: Long, pageable: Pageable, search: String): Page<Array<Any>>
}
