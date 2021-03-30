package io.tolgee.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface RepositoryRepository : JpaRepository<io.tolgee.model.Repository, Long> {
    @Query("""from Repository r 
        left join fetch Permission p on p.repository = r and p.user.id = :userAccountId
        left join fetch Organization o on r.organizationOwner = o
        left join fetch OrganizationMemberRole role on role.organization = o and role.user.id = :userAccountId
        where p is not null or (role is not null)
        """)
    fun findAllPermitted(userAccountId: Long): List<Array<Any>>
}
