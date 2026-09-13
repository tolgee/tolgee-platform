package io.tolgee.security.oauth2

import io.tolgee.api.v2.controllers.oauth2.OAuth2AuthorizationServerController
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.bearerHeaders
import io.tolgee.security.oauth2.OAuth2Constants
import io.tolgee.security.ratelimit.RateLimited
import io.tolgee.testing.AbstractControllerTest
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post

/** Boots the full application context to verify the OAuth2 endpoints are served under the main security chain. */
class OAuth2AuthorizationServerTest : AbstractControllerTest() {
  @Test
  fun `CORS is offered on the endpoints a client fetches, and never on authorize`() {
    // OAuth 2.1 §3.1: CORS "MUST NOT be supported at the Authorization Endpoint".
    corsHeader("/oauth2/authorize").assert.isNull()
    corsHeader("/oauth2/token").assert.isEqualTo("*")
    corsHeader("/oauth2/revoke").assert.isEqualTo("*")
    corsHeader("/.well-known/oauth-authorization-server").assert.isEqualTo("*")
  }

  @Test
  fun `publishes no JWK set`() {
    // An unknown path is answered by the SPA catch-all, so assert on the content, not the status.
    val response = mvc.perform(get("/oauth2/jwks")).andReturn().response
    (response.contentType ?: "").assert.doesNotContain("json")
    response.contentAsString.assert.doesNotContain("\"keys\"")
  }

  @Test
  fun `serves the authorization server metadata`() {
    val result =
      mvc
        .perform(get("/.well-known/oauth-authorization-server"))
        .andIsOk
        .andAssertThatJson {
          node("authorization_endpoint").isString
          node("token_endpoint").isString
          node("revocation_endpoint").isString.endsWith("/oauth2/revoke")
          node("revocation_endpoint_auth_methods_supported").isArray.containsExactly("none")
          node("jwks_uri").isAbsent()
        }.andReturn()

    // A shared cache keying on path alone must not serve one deployment's issuer and endpoint URLs to another's.
    result.response
      .getHeader("Cache-Control")
      .assert
      .isEqualTo("no-store")
  }

  @Test
  fun `sets a referrer policy on the authorization endpoint`() {
    // /oauth2/authorize redirects to URLs carrying `code` and `state`, so the referrer must not leak them.
    val response = mvc.perform(get("/oauth2/authorize")).andReturn().response
    response.getHeader("Referrer-Policy").assert.isEqualTo("strict-origin-when-cross-origin")
  }

  @Test
  fun `a stale bearer token does not break the endpoints a client uses to recover`() {
    // The 401 that sends a client to discovery carries its old token; if the filter ran here, fetching the metadata
    // or refreshing would answer 401 too and the client would have no way back.
    val stale = bearerHeaders("tgoat_expired-or-revoked")

    performGet("/.well-known/oauth-authorization-server", stale).andIsOk
    performGet("/.well-known/oauth-protected-resource/mcp/developer", stale).andIsOk
    mvc
      .perform(
        post(OAuth2Constants.TOKEN_PATH)
          .header("Authorization", "Bearer tgoat_expired-or-revoked")
          .param("grant_type", "refresh_token")
          .param("client_id", OAuth2Constants.BROWSER_EXTENSION_CLIENT_ID)
          .param("refresh_token", "tgort_whatever")
          .contentType(MediaType.APPLICATION_FORM_URLENCODED),
      ).andReturn()
      .response.status.assert
      .isEqualTo(400)
  }

  @Test
  fun `the token and revocation endpoints declare a rate limit, which nothing else on this path enforces`() {
    listOf("token", "revoke").forEach { method ->
      val annotation =
        OAuth2AuthorizationServerController::class.java.declaredMethods
          .single { it.name == method }
          .getAnnotation(RateLimited::class.java)

      annotation.assert.withFailMessage("$method must declare @RateLimited").isNotNull()
      annotation.isAuthentication.assert.isTrue()
      annotation.limit.assert.isGreaterThan(0)
    }
  }

  private fun corsHeader(path: String): String? =
    mvc
      .perform(options(path).header("Origin", "https://ext.example").header("Access-Control-Request-Method", "GET"))
      .andReturn()
      .response
      .getHeader("Access-Control-Allow-Origin")
}
