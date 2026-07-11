package io.tolgee.service

import io.tolgee.constants.Message
import io.tolgee.dtos.sso.SsoTenantConfig
import io.tolgee.dtos.sso.SsoTenantDto
import io.tolgee.exceptions.NotFoundException
import io.tolgee.exceptions.NotImplementedInOss
import io.tolgee.model.Organization
import io.tolgee.model.SsoTenant
import org.springframework.stereotype.Service

@Service
class TenantServiceOssStub : TenantService {
  override fun getEnabledConfigByDomainOrNull(domain: String?): SsoTenantConfig? {
    return null
  }

  override fun getEnabledConfigByDomain(domain: String?): SsoTenantConfig {
    throw NotFoundException(Message.SSO_DOMAIN_NOT_FOUND_OR_DISABLED)
  }

  override fun isSsoForcedForDomain(domain: String?): Boolean {
    return false
  }

  override fun save(tenant: SsoTenant): SsoTenant {
    // no-op
    throw NotImplementedInOss()
  }

  override fun saveAll(tenants: Iterable<SsoTenant>): List<SsoTenant> {
    // no-op
    if (tenants.any { true }) {
      // isn't empty
      throw NotImplementedInOss()
    }
    return emptyList()
  }

  override fun findAll(): List<SsoTenant> {
    // no-op
    return emptyList()
  }

  override fun findTenant(organizationId: Long): SsoTenant? {
    // no-op
    return null
  }

  override fun getTenant(organizationId: Long): SsoTenant {
    // no-op
    throw NotImplementedInOss()
  }

  override fun createOrUpdate(
    request: SsoTenantDto,
    organization: Organization,
    allowChangeDomain: Boolean,
  ): SsoTenant {
    // no-op
    throw NotImplementedInOss()
  }
}
