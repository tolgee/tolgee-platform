package io.tolgee.security.oauth2

import io.tolgee.fixtures.andIsOk
import io.tolgee.testing.assert
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

/**
 * The RFC 8414 discovery document, and the query encoding both legs of the flow depend on.
 */
class OAuth2DiscoveryConformanceTest : AbstractOAuth2ConformanceTest() {
  @Test
  fun `the discovery document describes what the server actually supports`() {
    mvc
      .perform(get("/.well-known/oauth-authorization-server"))
      .andIsOk
      .andReturn()
      .let { json(it) }
      .let { doc ->
        assertThat(values(doc, "response_types_supported")).containsExactly("code")
        assertThat(values(doc, "grant_types_supported"))
          .containsExactlyInAnyOrder("authorization_code", "refresh_token")
        assertThat(values(doc, "code_challenge_methods_supported")).containsExactly("S256")
        assertThat(values(doc, "token_endpoint_auth_methods_supported")).containsExactly("none")
        assertThat(values(doc, "scopes_supported")).contains("translations.view")
        doc
          .get("authorization_response_iss_parameter_supported")
          .asBoolean()
          .assert
          .isTrue()
        doc
          .get("authorization_endpoint")
          .asString()
          .assert
          .endsWith("/oauth2/authorize")
        doc
          .get("token_endpoint")
          .asString()
          .assert
          .endsWith("/oauth2/token")
        doc
          .get("issuer")
          .asString()
          .assert
          .isNotBlank()
      }
  }

  @Test
  fun `a state carrying reserved characters survives to the consent redirect`() {
    // RFC 6749 section 4.1.1 allows any printable ASCII in state, so a client that does not percent-encode its own
    // state must still be accepted rather than failing URI verification after the request was already validated.
    val state = "50%off [b] |c"
    val location =
      driver
        .authorize(CLIENT_ID, REDIRECT, validParams() + ("state" to state))
        .andReturn()
        .response
        .getHeader("Location")!!

    location.assert.contains(OAuth2Constants.CONSENT_PAGE_PATH)
    URLDecoder.decode(driver.queryParam(location, "state")!!, StandardCharsets.UTF_8).assert.isEqualTo(state)
  }

  @Test
  fun `the outbound encoder never emits a bare plus, so a space and a plus stay distinguishable`() {
    // RFC 6749 Appendix B: the authorize query is application/x-www-form-urlencoded, so a conforming client already
    // sends a space as `+` and a literal plus as `%2B`, and the container decodes both before Tolgee sees them.
    // Why the way out must not use a bare `+` is on OAuth2Redirects.encodeQueryValue.
    assertEncodedBothLegs(received = "abc+def", encoded = "abc%2Bdef")
    assertEncodedBothLegs(received = "abc def", encoded = "abc%20def")
  }

  @Test
  fun `a token request repeating a parameter is refused`() {
    val issued = json(tokenResult())
    val result =
      mvc
        .perform(
          post("/oauth2/token")
            .param("grant_type", "refresh_token")
            .param("client_id", CLIENT_ID)
            .param("refresh_token", issued.get("refresh_token").asString())
            .param("refresh_token", "a-second-value")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED),
        ).andReturn()

    result.response.status.assert
      .isEqualTo(400)
    json(result)
      .get("error")
      .asString()
      .assert
      .isEqualTo("invalid_request")
  }

  @Test
  fun `a state too long to be stored is refused on the redirect rather than at consent`() {
    val location =
      driver
        .authorize(
          CLIENT_ID,
          REDIRECT,
          validParams() + ("state" to "x".repeat(OAuth2AuthorizationService.MAX_STATE_LENGTH + 1)),
        ).andReturn()
        .response
        .getHeader("Location")!!

    location.assert.startsWith(REDIRECT)
    location.assert.contains("error=invalid_request")
  }

  @Test
  fun `a state of exactly the stored width survives the whole flow`() {
    // Pins MAX_STATE_LENGTH against the client_state column: if either moves alone this fails here instead of as a
    // runtime insert error on /oauth2/authorize.
    val state = "x".repeat(OAuth2AuthorizationService.MAX_STATE_LENGTH)
    val pending = driver.startPendingConsent(jwt(), CLIENT_ID, REDIRECT, clientState = state)
    val location = driver.consentRedirect(pending)

    location.assert.contains("code=")
    location.assert.contains("state=$state")
  }

  @Test
  fun `a repeated scope parameter is refused`() {
    val location =
      mvc
        .perform(
          get(
            "/oauth2/authorize?response_type=code&client_id=$CLIENT_ID" +
              "&redirect_uri=$REDIRECT" +
              "&scope=translations.view&scope=admin" +
              "&state=s1" +
              "&code_challenge=${OAuth2FlowDriver.s256Challenge(OAuth2FlowDriver.randomVerifier())}" +
              "&code_challenge_method=S256",
          ),
        ).andReturn()
        .response
        .getHeader("Location")!!

    location.assert.startsWith(REDIRECT)
    location.assert.contains("error=invalid_request")
    location.assert.contains("state=s1")
  }
}
