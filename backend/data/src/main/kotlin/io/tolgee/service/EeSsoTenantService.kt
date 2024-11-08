package io.tolgee.service

import io.tolgee.model.SsoTenant

interface EeSsoTenantService {
  fun getByDomain(domain: String): SsoTenant
}
