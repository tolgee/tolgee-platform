package io.tolgee.ee.data

import io.tolgee.ee.model.SsoTenant

data class SsoTenantDto(
  val authorizationUri: String,
  val clientId: String,
  val clientSecret: String,
  val redirectUri: String,
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
    redirectUri = this.redirectUriBase,
    tokenUri = this.tokenUri,
    isEnabled = this.isEnabledForThisOrganization,
    jwkSetUri = this.jwkSetUri,
    domainName = this.domain,
  )
