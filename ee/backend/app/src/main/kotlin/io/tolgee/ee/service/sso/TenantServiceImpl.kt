package io.tolgee.ee.service.sso

import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.constants.Message
import io.tolgee.dtos.sso.SsoTenantConfig
import io.tolgee.dtos.sso.SsoTenantDto
import io.tolgee.ee.repository.TenantRepository
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.Organization
import io.tolgee.model.SsoTenant
import io.tolgee.service.TenantService
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service

@Primary
@Service
class TenantServiceImpl(
  private val tenantRepository: TenantRepository,
  private val properties: TolgeeProperties,
) : TenantService {
  override fun getEnabledConfigByDomainOrNull(domain: String): SsoTenantConfig? {
    return properties.authentication.ssoGlobal
      .takeIf { it.enabled && domain == it.domain }
      ?.let { ssoTenantProperties -> SsoTenantConfig(ssoTenantProperties, null) }
      ?: domain
        .takeIf { properties.authentication.ssoOrganizations.enabled }
        ?.takeIf { properties.authentication.ssoOrganizations.isAllowedDomain(it) }
        ?.let {
          tenantRepository.findEnabledByDomain(it)?.let { ssoTenantEntity ->
            SsoTenantConfig(ssoTenantEntity, ssoTenantEntity.organization)
          }
        }
  }

  override fun getEnabledConfigByDomain(domain: String): SsoTenantConfig {
    return getEnabledConfigByDomainOrNull(domain) ?: throw NotFoundException(Message.SSO_DOMAIN_NOT_FOUND_OR_DISABLED)
  }

  override fun save(tenant: SsoTenant): SsoTenant = tenantRepository.save(tenant)

  override fun saveAll(tenants: Iterable<SsoTenant>): List<SsoTenant> = tenantRepository.saveAll(tenants)

  override fun findAll(): List<SsoTenant> = tenantRepository.findAll()

  override fun findTenant(organizationId: Long): SsoTenant? = tenantRepository.findByOrganizationId(organizationId)

  override fun getTenant(organizationId: Long): SsoTenant = findTenant(organizationId) ?: throw NotFoundException()

  override fun createOrUpdate(
    request: SsoTenantDto,
    organization: Organization,
  ): SsoTenant {
    val tenant = findTenant(organization.id) ?: SsoTenant()
    setTenantsFields(tenant, request, organization)
    return save(tenant)
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
    tenant.enabled = dto.enabled
  }
}
