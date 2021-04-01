package io.tolgee.repository

import io.tolgee.model.OrganizationRole
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface OrganizationRoleRepository : JpaRepository<OrganizationRole, Long> {
    fun findOneByUserIdAndOrganizationId(userId: Long, organizationId: Long): OrganizationRole?
}
