package io.tolgee.security.oauth2

import io.tolgee.constants.Message
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test

/**
 * The `/oauth2/authorize` half of the OAuth 2.1 contract: which requests reach the consent screen, which are
 * refused outright, and which come back as an error redirect.
 */
class OAuth2AuthorizeConformanceTest : AbstractOAuth2ConformanceTest() {
  @Test
  fun `authorize refuses an unregistered redirect_uri without redirecting anywhere`() {
    val response = driver.authorize(CLIENT_ID, "https://attacker.test/steal").andReturn().response
    response.status.assert.isEqualTo(400)
    response.getHeader("Location").assert.isNull()
  }

  @Test
  fun `authorize refuses a redirect_uri that only differs from the registered one by a suffix`() {
    val response = driver.authorize(CLIENT_ID, "$REDIRECT.attacker.test").andReturn().response
    response.status.assert.isEqualTo(400)
    response.getHeader("Location").assert.isNull()
  }

  @Test
  fun `authorize refuses an unknown client_id without redirecting anywhere`() {
    val response = driver.authorize("no-such-client", REDIRECT).andReturn().response
    response.status.assert.isEqualTo(400)
    response.getHeader("Location").assert.isNull()
  }

  @Test
  fun `opening an authorization for an unregistered client is refused`() {
    val result = driver.startAuthorization(jwt(), "no-such-client", REDIRECT, validParams()).andReturn()

    result.response.status.assert
      .isEqualTo(404)
    result.response.contentAsString.assert
      .contains(Message.OAUTH_UNKNOWN_CLIENT.code)
  }

  @Test
  fun `opening an authorization for a redirect_uri the client does not own is refused`() {
    // The SPA re-sends parameters that GET /oauth2/authorize already checked, so this endpoint has to check them
    // again — otherwise a crafted POST could open an authorization that redirects anywhere.
    val result =
      driver.startAuthorization(jwt(), CLIENT_ID, "https://attacker.test/steal", validParams()).andReturn()

    result.response.status.assert
      .isEqualTo(400)
    result.response.contentAsString.assert
      .contains(Message.OAUTH_REDIRECT_URI_NOT_REGISTERED.code)
  }

  @Test
  fun `a malformed authorize request is answered by the authorization endpoint itself, before any login`() {
    val location =
      driver
        .authorize(CLIENT_ID, REDIRECT, validParams() + mapOf("response_type" to "token", "state" to "s1"))
        .andReturn()
        .response
        .getHeader("Location")!!

    location.assert.startsWith(REDIRECT)
    location.assert.contains("error=unsupported_response_type")
    location.assert.contains("state=s1")
  }

  @Test
  fun `authorize hands a valid request to the consent screen without authenticating anybody`() {
    val response =
      driver
        .authorize(
          CLIENT_ID,
          REDIRECT,
          mapOf(
            "response_type" to "code",
            "scope" to "translations.view",
            "state" to "client-state",
            "code_challenge" to OAuth2FlowDriver.s256Challenge(OAuth2FlowDriver.randomVerifier()),
            "code_challenge_method" to "S256",
            "project" to "7",
          ),
        ).andReturn()
        .response
    response.status.assert.isEqualTo(302)
    val location = response.getHeader("Location")!!
    location.assert.contains(OAuth2Constants.CONSENT_PAGE_PATH)
    location.assert.contains("state=client-state")
    location.assert.contains("scope=translations.view")
    // The only hop that carries the client's project hint to the screen; the SPA reads it back off this URL.
    location.assert.contains("project=7")
  }

  @Test
  fun `the consent redirect is relative when no front-end url is configured`() {
    val original = tolgeeProperties.frontEndUrl
    tolgeeProperties.frontEndUrl = null
    try {
      authorizeRedirect().assert.startsWith(OAuth2Constants.CONSENT_PAGE_PATH)
    } finally {
      tolgeeProperties.frontEndUrl = original
    }
  }

  @Test
  fun `the consent redirect is absolute when a front-end url says where the SPA lives`() {
    val original = tolgeeProperties.frontEndUrl
    tolgeeProperties.frontEndUrl = "https://app.tolgee.example.com"
    try {
      val location = authorizeRedirect()
      location.assert.startsWith("https://app.tolgee.example.com${OAuth2Constants.CONSENT_PAGE_PATH}")
    } finally {
      tolgeeProperties.frontEndUrl = original
    }
  }

  @Test
  fun `opening an authorization with a scope the server does not support yields invalid_scope`() {
    errorRedirect(mapOf("scope" to "not.a.tolgee.scope")).assert.contains("error=invalid_scope")
  }

  @Test
  fun `omitting response_type is invalid_request, not unsupported_response_type`() {
    errorRedirect(mapOf("response_type" to null)).assert.contains("error=invalid_request")
  }

  @Test
  fun `opening an authorization with a response_type other than code is refused`() {
    errorRedirect(mapOf("response_type" to "token")).assert.contains("error=unsupported_response_type")
  }

  @Test
  fun `the plain PKCE method is refused`() {
    errorRedirect(mapOf("code_challenge_method" to "plain")).assert.contains("error=invalid_request")
  }

  @Test
  fun `an authorize request without a code_challenge is refused`() {
    errorRedirect(mapOf("code_challenge" to null)).assert.contains("error=invalid_request")
  }

  @Test
  fun `a code_challenge that is not a base64url S256 digest is refused`() {
    errorRedirect(mapOf("code_challenge" to "too-short")).assert.contains("error=invalid_request")
  }
}
