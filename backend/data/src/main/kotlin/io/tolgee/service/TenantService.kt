package io.tolgee.service

import io.tolgee.constants.Message
import io.tolgee.dtos.sso.SsoTenantConfig
import io.tolgee.dtos.sso.SsoTenantDto
import io.tolgee.exceptions.AuthenticationException
import io.tolgee.model.Organization
import io.tolgee.model.SsoTenant

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

  fun checkSsoNotRequired(username: String) {
    val domain = username.takeIf { it.count { it == '@' } == 1 }?.split('@')?.get(1)
    if (domain != null) {
      val tenant = getEnabledConfigByDomainOrNull(domain)
      if (tenant != null) {
        throw AuthenticationException(Message.SSO_LOGIN_FORCED_FOR_THIS_ACCOUNT, listOf(domain))
      }
    }
  }
}
