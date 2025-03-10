package io.tolgee.security.thirdParty.data

import io.tolgee.dtos.sso.SsoTenantConfig

data class OAuthUserDetails(
  var sub: String,
  var name: String? = null,
  var givenName: String? = null,
  var familyName: String? = null,
  var email: String = "",
  val refreshToken: String? = null,
  val tenant: SsoTenantConfig? = null,
)
