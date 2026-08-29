package io.tolgee.hateoas.oauth2

/** Where the consent screen must send the browser to hand the result back to the OAuth client. */
data class OAuth2RedirectModel(
  val redirectUrl: String,
)
