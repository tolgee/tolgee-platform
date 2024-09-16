package io.tolgee.ee.data

import io.tolgee.ee.model.Tenant

data class TenantDto(
  val authorizationUri: String,
  val clientId: String,
  val clientSecret: String,
  val redirectUri: String,
  val tokenUri: String,
)

fun Tenant.toDto(): TenantDto {
  return TenantDto(
    authorizationUri = this.authorizationUri,
    clientId = this.clientId,
    clientSecret = this.clientSecret,
    redirectUri = this.redirectUriBase,
    tokenUri = this.tokenUri,
  )
}
