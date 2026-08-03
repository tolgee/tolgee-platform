package io.tolgee.dtos.sso

import io.tolgee.api.ISsoTenant
import java.net.URI

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
  val organizationId: Long? = null,
) : ISsoTenant {
  /**
   * Google rejects the standard OIDC `offline_access` scope with `invalid_scope`;
   * refresh tokens must be requested via `access_type=offline&prompt=consent` instead,
   * and Google's refresh responses don't rotate the refresh token.
   */
  val isGoogle: Boolean
    get() = isGoogleHost(authorizationUri) || isGoogleHost(tokenUri)

  private fun isGoogleHost(uri: String): Boolean {
    val host = runCatching { URI(uri).host }.getOrNull() ?: return false
    return host == "accounts.google.com" || host == "googleapis.com" || host.endsWith(".googleapis.com")
  }

  constructor(other: ISsoTenant, organizationId: Long?) : this(
    clientId = other.clientId,
    clientSecret = other.clientSecret,
    authorizationUri = other.authorizationUri,
    domain = other.domain,
    tokenUri = other.tokenUri,
    force = other.force,
    global = other.global,
    organizationId = organizationId,
  )
}
