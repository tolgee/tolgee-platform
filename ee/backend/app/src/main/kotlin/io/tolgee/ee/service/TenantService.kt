package io.tolgee.ee.service

import io.tolgee.ee.data.CreateProviderRequest
import io.tolgee.ee.model.Tenant
import io.tolgee.ee.repository.TenantRepository
import io.tolgee.exceptions.NotFoundException
import org.springframework.stereotype.Service

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

  fun save(dto: CreateProviderRequest): Tenant {
    val tenant = Tenant()
    tenant.name = dto.name
    tenant.ssoProvider = dto.ssoProvider
    tenant.clientId = dto.clientId
    tenant.clientSecret = dto.clientSecret
    tenant.authorizationUri = dto.authorizationUri
    tenant.tokenUri = dto.tokenUri
    tenant.jwkSetUri = dto.jwkSetUri
    tenant.domain = dto.domain
    tenant.redirectUriBase = dto.redirectUri
    return save(tenant)
  }
}
