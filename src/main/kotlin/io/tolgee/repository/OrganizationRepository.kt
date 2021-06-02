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
    fun getOneBySlug(slug: String): Organization?

    @Query("""select o.id as id, o.name as name, o.description as description, o.slug as slug,
        o.basePermissions as basePermissions, r.type as currentUserRole 
        from Organization o 
        join OrganizationRole r on r.user.id = :userId and r.organization = o and (r.type = :roleType or :roleType is null)""")
    fun findAllPermitted(userId: Long?, pageable: Pageable, roleType: OrganizationRoleType? = null): Page<OrganizationView>

    fun countAllBySlug(slug: String): Long
    fun findAllByName(name: String): List<Organization>
}
