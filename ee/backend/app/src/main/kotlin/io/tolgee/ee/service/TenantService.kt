package io.tolgee.ee.service

import io.tolgee.ee.data.CreateProviderRequest
import io.tolgee.ee.model.Tenant
import io.tolgee.ee.repository.TenantRepository
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.NotFoundException
import org.springframework.stereotype.Service
import java.net.URI
import java.net.URISyntaxException

@Service
class TenantService(
  private val tenantRepository: TenantRepository,
) {
  fun getById(id: Long): Tenant {
    return tenantRepository.findById(id).orElseThrow { NotFoundException() }
  }

  fun getByDomain(domain: String): Tenant {
    return tenantRepository.findByDomain(domain) ?: throw NotFoundException()
  }

  fun save(tenant: Tenant): Tenant {
    return tenantRepository.save(tenant)
  }

  fun findAll(): List<Tenant> {
    return tenantRepository.findAll()
  }

  fun save(
    dto: CreateProviderRequest,
    organizationId: Long,
  ): Tenant {
    val tenant = Tenant()
    tenant.name = dto.name ?: ""
    tenant.organizationId = organizationId
    tenant.domain = extractDomain(dto.authorizationUri)
    tenant.clientId = dto.clientId
    tenant.clientSecret = dto.clientSecret
    tenant.authorizationUri = dto.authorizationUri
    tenant.tokenUri = dto.tokenUri
    tenant.redirectUriBase = dto.redirectUri.removeSuffix("/")
    tenant.isEnabledForThisOrganization = dto.isEnabled
    tenant.jwkSetUri = dto.jwkSetUri
    return save(tenant)
  }

  private fun extractDomain(authorizationUri: String): String {
    return try {
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
  }

  fun findTenant(organizationId: Long): Tenant? {
    return tenantRepository.findByOrganizationId(organizationId)
  }

  fun saveOrUpdate(request: CreateProviderRequest, organizationId: Long): Tenant {
    val tenant = findTenant(organizationId)
    return if (tenant == null) {
      save(request, organizationId)
    } else {
      tenant.name = request.name ?: ""
      tenant.clientId = request.clientId
      tenant.clientSecret = request.clientSecret
      tenant.authorizationUri = request.authorizationUri
      tenant.tokenUri = request.tokenUri
      tenant.redirectUriBase = request.redirectUri.removeSuffix("/")
      tenant.jwkSetUri = request.jwkSetUri
      tenant.isEnabledForThisOrganization = request.isEnabled
      save(tenant)
    }
  }
}
