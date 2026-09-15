package io.tolgee.security.oauth2

import io.tolgee.mcp.McpConstants
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

/**
 * RFC 8707 resource indicators: the `resource` parameter binds a grant to the one resource server its tokens will be
 * presented to, and the token endpoint refuses to mint a token for any other. Enforcement at the resource servers
 * themselves is covered by the access-token auth tests, not here.
 */
class OAuth2ResourceConformanceTest : AbstractOAuth2ConformanceTest() {
  @Autowired
  private lateinit var issuerResolver: OAuth2IssuerResolver

  private val apiResource get() = issuerResolver.issuerUrl
  private val mcpResource get() = issuerResolver.issuerUrl + McpConstants.DEVELOPER_ENDPOINT_PATH

  @Test
  fun `an authorize request naming an unknown resource error-redirects with invalid_target`() {
    val location =
      driver
        .authorize(CLIENT_ID, REDIRECT, validParams() + ("resource" to "https://elsewhere.example"))
        .andReturn()
        .response
        .getHeader("Location")!!

    location.assert.startsWith(REDIRECT)
    location.assert.contains("error=invalid_target")
  }

  @Test
  fun `the authorize redirect forwards the resource to the consent page`() {
    val location =
      driver
        .authorize(CLIENT_ID, REDIRECT, validParams() + ("resource" to mcpResource))
        .andReturn()
        .response
        .getHeader("Location")!!

    val forwarded = driver.queryParam(location, "resource")
    forwarded.assert.withFailMessage("the consent redirect dropped the resource: %s", location).isNotNull()
    URLDecoder.decode(forwarded!!, StandardCharsets.UTF_8).assert.isEqualTo(mcpResource)
  }

  @Test
  fun `the consent screen's authorize call refuses an unknown resource with invalid_target`() {
    errorRedirect(mapOf("resource" to "https://elsewhere.example")).assert.contains("error=invalid_target")
  }

  @Test
  fun `a grant minted for the MCP resource exchanges with a matching resource`() {
    val pending = driver.startPendingConsent(jwt(), CLIENT_ID, REDIRECT, resource = mcpResource)
    val code = driver.queryParam(driver.consentRedirect(pending), "code")!!

    val result = driver.exchangeCode(code, CLIENT_ID, REDIRECT, pending.verifier, resource = mcpResource).andReturn()

    result.response.status.assert
      .isEqualTo(200)
    json(result)
      .get("access_token")
      .asString()
      .assert
      .isNotBlank()
  }

  @Test
  fun `a code minted for the MCP resource cannot be exchanged for the API resource`() {
    val pending = driver.startPendingConsent(jwt(), CLIENT_ID, REDIRECT, resource = mcpResource)
    val code = driver.queryParam(driver.consentRedirect(pending), "code")!!

    val result = driver.exchangeCode(code, CLIENT_ID, REDIRECT, pending.verifier, resource = apiResource).andReturn()

    assertOAuthError(result, "invalid_target")
  }

  @Test
  fun `a code minted without a resource is an API grant and cannot become an MCP token`() {
    val pending = driver.startPendingConsent(jwt(), CLIENT_ID, REDIRECT)
    val code = driver.queryParam(driver.consentRedirect(pending), "code")!!

    val result = driver.exchangeCode(code, CLIENT_ID, REDIRECT, pending.verifier, resource = mcpResource).andReturn()

    assertOAuthError(result, "invalid_target")
  }

  @Test
  fun `a token request that names no resource works against any grant`() {
    val pending = driver.startPendingConsent(jwt(), CLIENT_ID, REDIRECT, resource = mcpResource)
    val code = driver.queryParam(driver.consentRedirect(pending), "code")!!

    val result = driver.exchangeCode(code, CLIENT_ID, REDIRECT, pending.verifier).andReturn()

    result.response.status.assert
      .isEqualTo(200)
  }

  @Test
  fun `an unknown resource at the token endpoint is invalid_target, never a 500`() {
    val pending = driver.startPendingConsent(jwt(), CLIENT_ID, REDIRECT)
    val code = driver.queryParam(driver.consentRedirect(pending), "code")!!

    val result =
      driver.exchangeCode(code, CLIENT_ID, REDIRECT, pending.verifier, resource = "not a resource").andReturn()

    assertOAuthError(result, "invalid_target")
  }

  @Test
  fun `a refresh naming a mismatching resource fails without hurting the grant`() {
    val pending = driver.startPendingConsent(jwt(), CLIENT_ID, REDIRECT, resource = mcpResource)
    val code = driver.queryParam(driver.consentRedirect(pending), "code")!!
    val tokens = json(driver.exchangeCode(code, CLIENT_ID, REDIRECT, pending.verifier).andReturn())
    val refreshToken = tokens.get("refresh_token").asString()

    assertOAuthError(driver.refresh(refreshToken, CLIENT_ID, resource = apiResource).andReturn(), "invalid_target")

    val refreshed = driver.refresh(refreshToken, CLIENT_ID, resource = mcpResource).andReturn()
    refreshed.response.status.assert
      .isEqualTo(200)
    json(refreshed)
      .get("access_token")
      .asString()
      .assert
      .isNotBlank()
  }

  @Test
  fun `a repeated resource parameter is refused at the token endpoint`() {
    val result =
      mvc
        .perform(
          post(OAuth2Constants.TOKEN_PATH)
            .param("grant_type", "refresh_token")
            .param("client_id", CLIENT_ID)
            .param("refresh_token", "tgort_whatever")
            .param("resource", apiResource, mcpResource)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED),
        ).andReturn()

    assertOAuthError(result, "invalid_request")
  }
}
