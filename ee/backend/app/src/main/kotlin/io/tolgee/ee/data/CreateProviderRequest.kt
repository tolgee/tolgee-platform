package io.tolgee.ee.data

data class CreateProviderRequest(
  val name: String,
  val ssoProvider: String,
  val clientId: String,
  val clientSecret: String,
  val authorizationUri: String,
  val tokenUri: String,
  val jwkSetUri: String,
  val domain: String,
  val redirectUri: String,
)
