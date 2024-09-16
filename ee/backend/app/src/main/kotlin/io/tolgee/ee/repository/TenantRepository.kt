package io.tolgee.ee.repository

import io.tolgee.ee.model.Tenant
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface TenantRepository : JpaRepository<Tenant, Long> {
  fun findByDomain(domain: String): Tenant?

  fun findByOrganizationId(id: Long): Tenant?
}
