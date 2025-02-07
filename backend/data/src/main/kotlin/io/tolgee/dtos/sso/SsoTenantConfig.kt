package io.tolgee.dtos.sso

import io.tolgee.api.ISsoTenant
import io.tolgee.model.Organization

data class SsoTenantConfig(
  override val clientId: String,
  override val clientSecret: String,
  override val authorizationUri: String,
  override val domain: String,
  override val tokenUri: String,
  /**
   * When true, users with an email matching the organization's domain must sign in using SSO
   */
  override val force: Boolean,
  override val global: Boolean,
  val organization: Organization? = null,
) : ISsoTenant {
  constructor(other: ISsoTenant, organization: Organization?) : this(
    clientId = other.clientId,
    clientSecret = other.clientSecret,
    authorizationUri = other.authorizationUri,
    domain = other.domain,
    tokenUri = other.tokenUri,
    force = other.force,
    global = other.global,
    organization = organization,
  )
}
