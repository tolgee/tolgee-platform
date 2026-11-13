package io.tolgee.security.oauth2

import io.tolgee.mcp.McpConstants
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class OAuth2ResourcesTest {
  private val resources = OAuth2Resources(mock { on { issuerUrl } doReturn ISSUER })

  @Test
  fun `no resource selects the REST API audience`() {
    resources.audienceFor(null).assert.isEqualTo(OAuth2Audience.API)
  }

  @Test
  fun `the issuer origin is the REST API resource`() {
    resources.audienceFor(ISSUER).assert.isEqualTo(OAuth2Audience.API)
  }

  @Test
  fun `the MCP endpoint URL is the MCP resource`() {
    resources.audienceFor(ISSUER + McpConstants.DEVELOPER_ENDPOINT_PATH).assert.isEqualTo(OAuth2Audience.MCP)
  }

  @Test
  fun `the advertised identifiers are the accepted ones`() {
    resources.audienceFor(resources.apiResource).assert.isEqualTo(OAuth2Audience.API)
    resources.audienceFor(resources.mcpResource).assert.isEqualTo(OAuth2Audience.MCP)
  }

  @Test
  fun `spellings a client may legitimately differ on still select the right audience`() {
    resources.audienceFor("$ISSUER/").assert.isEqualTo(OAuth2Audience.API)
    resources.audienceFor(ISSUER + McpConstants.DEVELOPER_ENDPOINT_PATH + "/").assert.isEqualTo(OAuth2Audience.MCP)
    resources.audienceFor("HTTPS://TOLGEE.EXAMPLE.COM").assert.isEqualTo(OAuth2Audience.API)
    resources.audienceFor("https://tolgee.example.com:443").assert.isEqualTo(OAuth2Audience.API)
  }

  @Test
  fun `a resource carrying a query or a fragment is refused, not normalised into a valid one`() {
    listOf(
      "$ISSUER#anything",
      "$ISSUER?x=1",
      ISSUER + McpConstants.DEVELOPER_ENDPOINT_PATH + "#anything",
      ISSUER + McpConstants.DEVELOPER_ENDPOINT_PATH + "?x=1",
    ).forEach { resource ->
      val error = assertThrows<OAuth2Error> { resources.audienceFor(resource) }
      error.error.assert
        .withFailMessage("expected invalid_target for %s, got %s", resource, error.error)
        .isEqualTo(OAuth2Error.INVALID_TARGET)
    }
  }

  @Test
  fun `any other resource is refused with invalid_target`() {
    listOf(
      "https://elsewhere.example",
      "$ISSUER/mcp",
      // The path is the one part that stays case-sensitive: a different path is a different resource.
      "$ISSUER/MCP/DEVELOPER",
      "${ISSUER}evil.example/mcp/developer",
      "not a url",
      "",
    ).forEach { resource ->
      val error = assertThrows<OAuth2Error> { resources.audienceFor(resource) }
      error.error.assert
        .withFailMessage("expected invalid_target for %s, got %s", resource, error.error)
        .isEqualTo(OAuth2Error.INVALID_TARGET)
    }
  }

  companion object {
    private const val ISSUER = "https://tolgee.example.com"
  }
}
