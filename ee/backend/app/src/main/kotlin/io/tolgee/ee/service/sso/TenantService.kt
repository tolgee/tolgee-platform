package io.tolgee.ee.service.sso

import io.tolgee.configuration.tolgee.SsoGlobalProperties
import io.tolgee.constants.Message
import io.tolgee.ee.data.CreateProviderRequest
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.SsoTenant
import io.tolgee.repository.TenantRepository
import io.tolgee.security.thirdParty.SsoTenantConfig
import io.tolgee.security.thirdParty.SsoTenantConfig.Companion.toConfig
import io.tolgee.service.organization.OrganizationService
import org.springframework.stereotype.Service

@Service
class TenantService(
  private val tenantRepository: TenantRepository,
  private val ssoGlobalProperties: SsoGlobalProperties,
  private val organizationService: OrganizationService,
) {
  fun getById(id: Long): SsoTenant = tenantRepository.findById(id).orElseThrow { NotFoundException() }

  fun getByDomain(domain: String): SsoTenant {
    return tenantRepository.findByDomain(domain) ?: throw NotFoundException()
  }

  fun getEnabledConfigByDomain(domain: String): SsoTenantConfig {
    return ssoGlobalProperties
      .takeIf { it.globalEnabled && domain == it.domain }
      ?.toConfig()
      ?: domain.let { tenantRepository.findEnabledByDomain(it)?.toConfig() }
      ?: throw NotFoundException(Message.SSO_DOMAIN_NOT_FOUND_OR_DISABLED)
  }

  fun save(tenant: SsoTenant): SsoTenant = tenantRepository.save(tenant)

  fun findAll(): List<SsoTenant> = tenantRepository.findAll()

  fun findTenant(organizationId: Long): SsoTenant? = tenantRepository.findByOrganizationId(organizationId)

  fun getTenant(organizationId: Long): SsoTenant = findTenant(organizationId) ?: throw NotFoundException()

  fun saveOrUpdate(
    request: CreateProviderRequest,
    organizationId: Long,
  ): SsoTenant {
    // TODO: pass organization directly
    val tenant = findTenant(organizationId) ?: SsoTenant()
    return setAndSaveTenantsFields(tenant, request, organizationId)
  }

  private fun setAndSaveTenantsFields(
    tenant: SsoTenant,
    dto: CreateProviderRequest,
    organizationId: Long,
  ): SsoTenant {
    tenant.name = dto.name ?: ""
    tenant.organization = organizationService.get(organizationId)
    tenant.domain = dto.domainName
    tenant.clientId = dto.clientId
    tenant.clientSecret = dto.clientSecret
    tenant.authorizationUri = dto.authorizationUri
    tenant.tokenUri = dto.tokenUri
    tenant.jwkSetUri = dto.jwkSetUri
    tenant.enabled = dto.isEnabled
    val saved = save(tenant)
    // TODO: don't update organization like this
    organizationService.updateSsoProvider(organizationId, saved)
    return saved
  }
}
