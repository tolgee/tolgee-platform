package io.tolgee.ee.data

import io.tolgee.model.SsoTenant

data class SsoTenantDto(
  val authorizationUri: String,
  val clientId: String,
  val clientSecret: String,
  val tokenUri: String,
  val isEnabled: Boolean,
  val jwkSetUri: String,
  val domainName: String,
)

fun SsoTenant.toDto(): SsoTenantDto =
  SsoTenantDto(
    authorizationUri = this.authorizationUri,
    clientId = this.clientId,
    clientSecret = this.clientSecret,
    tokenUri = this.tokenUri,
    isEnabled = this.enabled,
    jwkSetUri = this.jwtSetUri,
    domainName = this.domain,
  )
