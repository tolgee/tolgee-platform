package io.tolgee.repository

import io.tolgee.model.OrganizationMemberRole
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface OrganizationMemberRoleRepository : JpaRepository<OrganizationMemberRole, Long> {
    fun findAllByUserIdAndOrganizationId(userId: Long, organizationId: Long): OrganizationMemberRole?
}
