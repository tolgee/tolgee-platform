package io.tolgee.ee.data

data class CreateProviderRequest(
  val name: String?,
  val clientId: String,
  val clientSecret: String,
  val authorizationUri: String,
  val redirectUri: String,
  val tokenUri: String,
)
