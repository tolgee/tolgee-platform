package io.tolgee.ee.service

import io.tolgee.configuration.tolgee.SsoGlobalProperties
import io.tolgee.constants.Message
import io.tolgee.ee.data.CreateProviderRequest
import io.tolgee.ee.exceptions.OAuthAuthorizationException
import io.tolgee.ee.repository.TenantRepository
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.SsoTenant
import io.tolgee.service.organization.OrganizationService
import org.springframework.stereotype.Service
import java.net.URI
import java.net.URISyntaxException

@Service
class TenantService(
  private val tenantRepository: TenantRepository,
  private val ssoGlobalProperties: SsoGlobalProperties,
  private val organizationService: OrganizationService
) {
  fun getById(id: Long): SsoTenant = tenantRepository.findById(id).orElseThrow { NotFoundException() }

  fun getEnabledByDomain(domain: String): SsoTenant =
    if (ssoGlobalProperties.enabled) {
      buildGlobalTenant()
    } else {
      tenantRepository.findEnabledByDomain(domain) ?: throw NotFoundException(Message.SSO_DOMAIN_NOT_FOUND_OR_DISABLED)
    }

  fun getByDomain(domain: String): SsoTenant =
    if (ssoGlobalProperties.enabled && domain == ssoGlobalProperties.domain) {
      buildGlobalTenant()
    } else {
      tenantRepository.findByDomain(domain) ?: throw NotFoundException()
    }

  private fun buildGlobalTenant(): SsoTenant =
    SsoTenant().apply {
      enabled = validateProperty(ssoGlobalProperties.enabled.toString(), "enabled").toBoolean()
      domain = validateProperty(ssoGlobalProperties.domain, "domain")
      clientId = validateProperty(ssoGlobalProperties.clientId, "clientId")
      clientSecret = validateProperty(ssoGlobalProperties.clientSecret, "clientSecret")
      authorizationUri = validateProperty(ssoGlobalProperties.authorizationUrl, "authorizationUrl")
      tokenUri = validateProperty(ssoGlobalProperties.tokenUrl, "tokenUrl")
      jwkSetUri = validateProperty(ssoGlobalProperties.jwkSetUri, "jwkSetUri")
    }

  private fun validateProperty(
    property: String?,
    propertyName: String,
  ): String =
    property ?: throw OAuthAuthorizationException(
      Message.SSO_GLOBAL_CONFIG_MISSING_PROPERTIES,
      "$propertyName is missing in global SSO configuration",
    )

  fun save(tenant: SsoTenant): SsoTenant = tenantRepository.save(tenant)

  fun findAll(): List<SsoTenant> = tenantRepository.findAll()

  private fun extractDomain(authorizationUri: String): String =
    try {
      val uri = URI(authorizationUri)
      val domain = uri.host
      val port = uri.port

      val domainWithPort =
        if (port != -1) {
          "$domain:$port"
        } else {
          domain
        }

      if (domainWithPort.startsWith("www.")) {
        domainWithPort.substring(4)
      } else {
        domainWithPort
      }
    } catch (e: URISyntaxException) {
      throw BadRequestException("Invalid authorization uri")
    }

  fun findTenant(organizationId: Long): SsoTenant? = tenantRepository.findByOrganizationId(organizationId)

  fun getTenant(organizationId: Long): SsoTenant = findTenant(organizationId) ?: throw NotFoundException()

  fun saveOrUpdate(
    request: CreateProviderRequest,
    organizationId: Long,
  ): SsoTenant {
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
    organizationService.updateSsoProvider(organizationId, saved)
    return saved
  }
}
