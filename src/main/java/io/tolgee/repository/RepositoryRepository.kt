package io.tolgee.repository

import io.tolgee.model.views.RepositoryView
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface RepositoryRepository : JpaRepository<io.tolgee.model.Repository, Long> {
    companion object {
        const val BASE_ALL_PERMITTED_QUERY = """select r.id as id, r.name as name, r.description as description,
        r.addressPart as addressPart, 
        ua as userOwner, o.name as organizationOwnerName, o.addressPart as organizationOwnerAddressPart, 
        o.basePermissions as organizationBasePermissions, role.type as organizationRole, p.type as directPermissions
        from Repository r 
        left join UserAccount ua on ua.id = r.userOwner.id
        left join Permission p on p.repository = r and p.user.id = :userAccountId
        left join Organization o on r.organizationOwner = o
        left join OrganizationRole role on role.organization = o and role.user.id = :userAccountId
        where (p is not null or role is not null)
        """
    }

    @Query("""from Repository r 
        left join fetch Permission p on p.repository = r and p.user.id = :userAccountId
        left join fetch Organization o on r.organizationOwner = o
        left join fetch OrganizationRole role on role.organization = o and role.user.id = :userAccountId
        where p is not null or (role is not null)
        """)
    fun findAllPermitted(userAccountId: Long): List<Array<Any>>

    @Query("""$BASE_ALL_PERMITTED_QUERY 
                and ((lower(r.name) like lower(concat('%', :search,'%'))
                or lower(o.name) like lower(concat('%', :search,'%')))
                or lower(ua.name) like lower(concat('%', :search,'%')) or :search is null)
    """)
    fun findAllPermitted(userAccountId: Long, pageable: Pageable, search: String? = null): Page<RepositoryView>

    fun findAllByOrganizationOwnerId(organizationOwnerId: Long): List<io.tolgee.model.Repository>

    @Query("""$BASE_ALL_PERMITTED_QUERY and o.id = :organizationOwnerId and o is not null
         and ((lower(r.name) like lower(concat('%', :search,'%'))
                or lower(o.name) like lower(concat('%', :search,'%')))
                or lower(ua.name) like lower(concat('%', :search,'%')) or :search is null)
        """)
    fun findAllPermittedInOrganization(userAccountId: Long, organizationOwnerId: Long, pageable: Pageable, search: String?): Page<RepositoryView>

    fun countAllByAddressPart(addressPart: String): Long

    @Query("""$BASE_ALL_PERMITTED_QUERY and r.id = :repositoryId""")
    fun findViewById(userAccountId: Long, repositoryId: Long): RepositoryView?

    fun findAllByName(name: String): List<io.tolgee.model.Repository>
}
