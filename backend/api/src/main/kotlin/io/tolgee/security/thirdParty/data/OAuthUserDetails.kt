package io.tolgee.security.thirdParty.data

data class OAuthUserDetails(
  var sub: String? = null,
  var name: String? = null,
  var givenName: String? = null,
  var familyName: String? = null,
  var email: String = "",
  val domain: String? = null,
  val organizationId: Long? = null,
  val refreshToken: String? = null,
)
