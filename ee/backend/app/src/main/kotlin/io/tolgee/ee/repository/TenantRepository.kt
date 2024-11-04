package io.tolgee.ee.repository

import io.tolgee.model.SsoTenant
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface TenantRepository : JpaRepository<SsoTenant, Long> {
  fun findByDomain(domain: String): SsoTenant?

  fun findByOrganizationId(id: Long): SsoTenant?

  @Query(
    "SELECT t FROM SsoTenant t WHERE t.enabled = true AND t.domain = :domain",
  )
  fun findEnabledByDomain(domain: String): SsoTenant?
}
