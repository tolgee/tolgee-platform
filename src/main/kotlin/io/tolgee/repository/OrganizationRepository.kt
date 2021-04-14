package io.tolgee.repository

import io.tolgee.model.Organization
import io.tolgee.model.enums.OrganizationRoleType
import io.tolgee.model.views.OrganizationView
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface OrganizationRepository : JpaRepository<Organization, Long> {
    fun getOneByAddressPart(addressPart: String): Organization?

    @Query("""select o.id as id, o.name as name, o.description as description, o.addressPart as addressPart,
        o.basePermissions as basePermissions, r.type as currentUserRole 
        from Organization o 
        join OrganizationRole r on r.user.id = :userId and r.organization = o and (r.type = :roleType or :roleType is null)""")
    fun findAllPermitted(userId: Long?, pageable: Pageable, roleType: OrganizationRoleType? = null): Page<OrganizationView>

    fun countAllByAddressPart(addressPart: String): Long
    fun findAllByName(name: String): List<Organization>
}
