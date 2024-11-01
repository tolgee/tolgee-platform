package io.tolgee.security.thirdParty.data

import io.tolgee.model.SsoTenant

data class OAuthUserDetails(
  var sub: String? = null,
  var name: String? = null,
  var givenName: String? = null,
  var familyName: String? = null,
  var email: String = "",
  val refreshToken: String? = null,
  val tenant: SsoTenant? = null,
)
