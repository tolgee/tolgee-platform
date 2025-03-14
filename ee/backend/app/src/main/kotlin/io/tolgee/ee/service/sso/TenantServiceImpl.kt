package io.tolgee.ee.service.sso

import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.constants.Caches
import io.tolgee.constants.Message
import io.tolgee.dtos.sso.SsoTenantConfig
import io.tolgee.dtos.sso.SsoTenantDto
import io.tolgee.ee.repository.TenantRepository
import io.tolgee.exceptions.NotFoundException
import io.tolgee.exceptions.PermissionException
import io.tolgee.model.Organization
import io.tolgee.model.SsoTenant
import io.tolgee.service.TenantService
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.context.annotation.Lazy
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service

@Primary
@Service
class TenantServiceImpl(
  private val tenantRepository: TenantRepository,
  private val properties: TolgeeProperties,
  @Suppress("SelfReferenceConstructorParameter") @Lazy
  private val self: TenantServiceImpl,
) : TenantService {
  override fun getEnabledConfigByDomainOrNull(domain: String?): SsoTenantConfig? {
    if (domain.isNullOrEmpty()) return null
    return self.getEnabledConfigByDomainOrNullCached(domain)
  }

  @Cacheable(Caches.SSO_TENANTS, key = "#domain")
  protected fun getEnabledConfigByDomainOrNullCached(domain: String): SsoTenantConfig? {
    return properties.authentication.ssoGlobal
      .takeIf { it.enabled && domain == it.domain }
      ?.let { ssoTenantProperties -> SsoTenantConfig(ssoTenantProperties, null) }
      ?: domain
        .takeIf { properties.authentication.ssoOrganizations.enabled }
        ?.let {
          tenantRepository.findEnabledByDomain(it)?.let { ssoTenantEntity ->
            SsoTenantConfig(ssoTenantEntity, ssoTenantEntity.organization.id)
          }
        }
  }

  override fun getEnabledConfigByDomain(domain: String?): SsoTenantConfig {
    return self.getEnabledConfigByDomainOrNull(domain)
      ?: throw NotFoundException(Message.SSO_DOMAIN_NOT_FOUND_OR_DISABLED)
  }

  override fun isSsoForcedForDomain(domain: String?): Boolean {
    val tenant = self.getEnabledConfigByDomainOrNull(domain)
    return tenant?.force == true
  }

  @CacheEvict(Caches.SSO_TENANTS, allEntries = true)
  override fun save(tenant: SsoTenant): SsoTenant = tenantRepository.save(tenant)

  @CacheEvict(Caches.SSO_TENANTS, allEntries = true)
  override fun saveAll(tenants: Iterable<SsoTenant>): List<SsoTenant> = tenantRepository.saveAll(tenants)

  override fun findAll(): List<SsoTenant> = tenantRepository.findAll()

  override fun findTenant(organizationId: Long): SsoTenant? = tenantRepository.findByOrganizationId(organizationId)

  override fun getTenant(organizationId: Long): SsoTenant = findTenant(organizationId) ?: throw NotFoundException()

  override fun createOrUpdate(
    request: SsoTenantDto,
    organization: Organization,
    allowChangeDomain: Boolean,
  ): SsoTenant {
    val tenant = findTenant(organization.id) ?: SsoTenant()
    if (!allowChangeDomain && tenant.domain != request.domain) {
      throw PermissionException(Message.SSO_DOMAIN_NOT_ALLOWED)
    }
    setTenantsFields(tenant, request, organization)
    return self.save(tenant)
  }

  private fun setTenantsFields(
    tenant: SsoTenant,
    dto: SsoTenantDto,
    organization: Organization,
  ) {
    tenant.organization = organization
    tenant.domain = dto.domain
    tenant.clientId = dto.clientId
    tenant.clientSecret = dto.clientSecret
    tenant.authorizationUri = dto.authorizationUri
    tenant.tokenUri = dto.tokenUri
    tenant.force = dto.force
    tenant.enabled = dto.enabled
  }
}
