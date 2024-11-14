package io.tolgee.security.thirdParty

import io.tolgee.api.ISsoTenant
import io.tolgee.model.Organization

data class SsoTenantConfig(
  override val name: String,
  override val clientId: String,
  override val clientSecret: String,
  override val authorizationUri: String,
  override val domain: String,
  override val jwtSetUri: String,
  override val tokenUri: String,
  override val global: Boolean,
  override val organization: Organization? = null,
) : ISsoTenant {
  constructor(other: ISsoTenant) : this(
    name = other.name,
    clientId = other.clientId,
    clientSecret = other.clientSecret,
    authorizationUri = other.authorizationUri,
    domain = other.domain,
    jwtSetUri = other.jwtSetUri,
    tokenUri = other.tokenUri,
    global = other.global,
    organization = other.organization,
  )
}
