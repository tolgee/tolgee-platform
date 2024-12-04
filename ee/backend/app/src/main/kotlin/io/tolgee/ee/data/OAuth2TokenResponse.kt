package io.tolgee.ee.data

@Suppress("PropertyName")
class OAuth2TokenResponse(
  val id_token: String,
  val scope: String,
  val refresh_token: String,
)
