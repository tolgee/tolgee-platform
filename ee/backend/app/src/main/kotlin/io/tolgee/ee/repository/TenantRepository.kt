package io.tolgee.ee.repository

import io.tolgee.model.SsoTenant
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface TenantRepository : JpaRepository<SsoTenant, Long> {
  fun findByDomain(domain: String): SsoTenant?

  fun findByOrganizationId(id: Long): SsoTenant?
}
