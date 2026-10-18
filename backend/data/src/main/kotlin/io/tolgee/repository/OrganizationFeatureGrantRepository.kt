package io.tolgee.repository

import io.tolgee.model.OrganizationFeatureGrant
import org.springframework.context.annotation.Lazy
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
@Lazy
interface OrganizationFeatureGrantRepository : JpaRepository<OrganizationFeatureGrant, Long> {
  fun findAllByOrganizationId(organizationId: Long): List<OrganizationFeatureGrant>
}
