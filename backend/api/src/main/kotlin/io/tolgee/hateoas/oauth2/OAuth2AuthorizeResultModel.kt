package io.tolgee.hateoas.oauth2

/**
 * Exactly one of the two is set: a [consentState] to render the screen with, or a [redirectUrl] carrying an RFC 6749
 * §4.1.2.1 error the browser must hand back to the client instead.
 */
data class OAuth2AuthorizeResultModel(
  val consentState: String?,
  val redirectUrl: String?,
)
