package io.tolgee.dtos.sso

import io.tolgee.api.ISsoTenant
import io.tolgee.model.SsoTenant

data class SsoTenantDto(
  val enabled: Boolean,
  override val authorizationUri: String,
  override val clientId: String,
  override val clientSecret: String,
  override val tokenUri: String,
  override val domain: String,
) : ISsoTenant {
  override val global: Boolean
    get() = false
}

fun SsoTenant.toDto(): SsoTenantDto =
  SsoTenantDto(
    authorizationUri = this.authorizationUri,
    clientId = this.clientId,
    clientSecret = this.clientSecret,
    tokenUri = this.tokenUri,
    enabled = this.enabled,
    domain = this.domain,
  )
