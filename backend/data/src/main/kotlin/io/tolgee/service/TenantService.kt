package io.tolgee.service

import io.tolgee.dtos.sso.SsoTenantConfig
import io.tolgee.dtos.sso.SsoTenantDto
import io.tolgee.model.Organization
import io.tolgee.model.SsoTenant
import io.tolgee.model.UserAccount

interface TenantService {
  fun getEnabledConfigByDomainOrNull(domain: String): SsoTenantConfig?

  fun getEnabledConfigByDomain(domain: String): SsoTenantConfig

  fun save(tenant: SsoTenant): SsoTenant

  fun saveAll(tenants: Iterable<SsoTenant>): List<SsoTenant>

  fun findAll(): List<SsoTenant>

  fun findTenant(organizationId: Long): SsoTenant?

  fun getTenant(organizationId: Long): SsoTenant

  fun createOrUpdate(
    request: SsoTenantDto,
    organization: Organization,
  ): SsoTenant

  fun checkSsoNotRequired(username: String)

  fun checkSsoNotRequiredOrAuthProviderChangeActive(userAccount: UserAccount)
}
