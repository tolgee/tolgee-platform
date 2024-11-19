package io.tolgee.ee.service.sso

import io.tolgee.configuration.tolgee.SsoGlobalProperties
import io.tolgee.configuration.tolgee.SsoOrganizationsProperties
import io.tolgee.constants.Message
import io.tolgee.ee.data.CreateProviderRequest
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.Organization
import io.tolgee.model.SsoTenant
import io.tolgee.repository.TenantRepository
import io.tolgee.security.thirdParty.SsoTenantConfig
import org.springframework.stereotype.Service

@Service
class TenantService(
  private val tenantRepository: TenantRepository,
  private val ssoGlobalProperties: SsoGlobalProperties,
  private val ssoOrganizationsProperties: SsoOrganizationsProperties,
) {
  fun getEnabledConfigByDomain(domain: String): SsoTenantConfig {
    return ssoGlobalProperties
      .takeIf { it.enabled && domain == it.domain }
      ?.let { ssoTenantProperties -> SsoTenantConfig(ssoTenantProperties, null) }
      ?: domain
        .takeIf { ssoOrganizationsProperties.enabled }
        ?.let {
          tenantRepository.findEnabledByDomain(it)?.let { ssoTenantEntity ->
            SsoTenantConfig(ssoTenantEntity, ssoTenantEntity.organization)
          }
        }
      ?: throw NotFoundException(Message.SSO_DOMAIN_NOT_FOUND_OR_DISABLED)
  }

  fun save(tenant: SsoTenant): SsoTenant = tenantRepository.save(tenant)

  fun findAll(): List<SsoTenant> = tenantRepository.findAll()

  fun findTenant(organizationId: Long): SsoTenant? = tenantRepository.findByOrganizationId(organizationId)

  fun getTenant(organizationId: Long): SsoTenant = findTenant(organizationId) ?: throw NotFoundException()

  fun createOrUpdate(
    request: CreateProviderRequest,
    organization: Organization,
  ): SsoTenant {
    val tenant = findTenant(organization.id) ?: SsoTenant()
    setTenantsFields(tenant, request, organization)
    return save(tenant)
  }

  private fun setTenantsFields(
    tenant: SsoTenant,
    dto: CreateProviderRequest,
    organization: Organization,
  ) {
    tenant.organization = organization
    tenant.domain = dto.domain
    tenant.clientId = dto.clientId
    tenant.clientSecret = dto.clientSecret
    tenant.authorizationUri = dto.authorizationUri
    tenant.tokenUri = dto.tokenUri
    tenant.jwkSetUri = dto.jwkSetUri
    tenant.enabled = dto.enabled
  }
}
