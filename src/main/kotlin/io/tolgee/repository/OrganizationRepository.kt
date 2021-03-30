package io.tolgee.repository

import io.tolgee.model.Organization
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface OrganizationRepository : JpaRepository<Organization, Long> {
    fun findByNameContainingIgnoreCase(name: String, pageable: Pageable): Page<Organization>

    fun getOneByAddressPart(addressPart: String): Organization?

    @Query("from Organization o join fetch OrganizationMemberRole r on r.user.id = :userId")
    fun findAllPermitted(userId: Long?, pageable: Pageable): Page<Organization>
}
