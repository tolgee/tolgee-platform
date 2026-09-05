package io.tolgee.security.oauth2

import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsOk
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test

class OAuth2ProtectedResourceMetadataTest : AuthorizedControllerTest() {
  @Test
  fun `serves RFC 9728 protected resource metadata for the MCP endpoint`() {
    performGet("/.well-known/oauth-protected-resource/mcp/developer")
      .andIsOk
      .andAssertThatJson {
        // Why the identifier is the MCP path and not the server root is on
        // ProtectedResourceMetadataController.mcpDeveloperMetadata.
        node("resource").isString.endsWith("/mcp/developer")
        // Clients append /.well-known/oauth-authorization-server to this, so it has to be a dereferenceable URL.
        node("authorization_servers").isArray.hasSize(1)
        node("authorization_servers[0]").isString.startsWith("http")
        node("scopes_supported").isArray.contains("translations.suggest")
      }
  }

  @Test
  fun `the protected-resource document is not cacheable`() {
    // Same hazard as the RFC 8414 document: it names the issuer, so a shared cache must not hand it to another
    // deployment's clients.
    val result = performGet("/.well-known/oauth-protected-resource/mcp/developer").andIsOk.andReturn()

    result.response
      .getHeader("Cache-Control")
      .assert
      .isEqualTo("no-store")
  }
}
