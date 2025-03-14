package io.tolgee.service

import io.tolgee.dtos.sso.SsoTenantConfig
import io.tolgee.dtos.sso.SsoTenantDto
import io.tolgee.model.Organization
import io.tolgee.model.SsoTenant

interface TenantService {
  fun getEnabledConfigByDomainOrNull(domain: String?): SsoTenantConfig?

  fun getEnabledConfigByDomain(domain: String?): SsoTenantConfig

  fun isSsoForcedForDomain(domain: String?): Boolean

  fun save(tenant: SsoTenant): SsoTenant

  fun saveAll(tenants: Iterable<SsoTenant>): List<SsoTenant>

  fun findAll(): List<SsoTenant>

  fun findTenant(organizationId: Long): SsoTenant?

  fun getTenant(organizationId: Long): SsoTenant

  fun createOrUpdate(
    request: SsoTenantDto,
    organization: Organization,
    allowChangeDomain: Boolean = false,
  ): SsoTenant
}
