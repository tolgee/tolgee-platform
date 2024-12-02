package io.tolgee.security.thirdParty

import io.tolgee.api.ISsoTenant
import io.tolgee.model.Organization

data class SsoTenantConfig(
  override val clientId: String,
  override val clientSecret: String,
  override val authorizationUri: String,
  override val domain: String,
  override val tokenUri: String,
  override val global: Boolean,
  val organization: Organization? = null,
) : ISsoTenant {
  constructor(other: ISsoTenant, organization: Organization?) : this(
    clientId = other.clientId,
    clientSecret = other.clientSecret,
    authorizationUri = other.authorizationUri,
    domain = other.domain,
    tokenUri = other.tokenUri,
    global = other.global,
    organization = organization,
  )
}
