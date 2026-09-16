package io.tolgee.security.oauth2

import com.sun.net.httpserver.HttpServer
import io.tolgee.mcp.McpConstants
import io.tolgee.testing.assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.test.context.TestPropertySource
import java.net.InetAddress
import java.net.InetSocketAddress

/**
 * The whole CIMD journey through the real HTTP stack: an unknown client presents an HTTPS (here, dev-loopback) URL as
 * its `client_id`, the authorization server fetches and validates the document at that URL, and a token is issued
 * against the unverified client it describes. SSRF protection is disabled so the metadata can be served on loopback,
 * which is exactly the dev escape hatch the fetcher documents.
 */
@TestPropertySource(properties = ["tolgee.internal.disable-url-ssrf-protection=true"])
class OAuth2CimdFlowTest : AbstractOAuth2FlowTest() {
  private var server: HttpServer? = null

  @AfterEach
  fun stopServer() {
    server?.stop(0)
    server = null
  }

  @Test
  fun `an unknown client resolves through its metadata document and is issued an MCP-audience token`() {
    val port = serveDocument { url -> validDocument(url) }
    val clientId = "http://127.0.0.1:$port/client"
    val redirect = "http://127.0.0.1:$port/cb"
    val mcpResource = issuerResolver.issuerUrl + McpConstants.DEVELOPER_ENDPOINT_PATH

    // The browser-facing authorize endpoint resolves the unknown client rather than rejecting it.
    driver
      .authorize(clientId, redirect, validParams(mcpResource))
      .andReturn()
      .response.status.assert
      .isEqualTo(302)

    val pending =
      driver.startPendingConsent(jwt(), clientId, redirect, scope = "translations.view", resource = mcpResource)
    val code = driver.queryParam(driver.consentRedirect(pending, projectId = null), "code")!!
    val token =
      json(driver.exchangeCode(code, clientId, redirect, pending.verifier, resource = mcpResource))
        .get("access_token")
        .asString()

    token.assert.isNotBlank()
    val grant = stored(token)
    grant.clientMetadataHash.assert.isNotNull()
    grant.boundAudience().assert.isEqualTo(OAuth2Audience.MCP)
  }

  @Test
  fun `a token request for a CIMD client whose document does not resolve is invalid_grant, not invalid_client`() {
    // The client's metadata host being unreachable must not read as "unknown client": a grant may exist, and the
    // grant lookup — not the third party's uptime — decides. Here no grant exists, so the answer is invalid_grant.
    json(driver.refresh("tgort_no-such-grant", "https://unresolvable.invalid/client"))
      .get("error")
      .asString()
      .assert
      .isEqualTo("invalid_grant")
  }

  @Test
  fun `a document whose client_id does not match the fetched URL is refused`() {
    val port = serveDocument { validDocument("https://not-this-url.example/client") }
    val clientId = "http://127.0.0.1:$port/client"

    driver
      .authorize(clientId, "http://127.0.0.1:$port/cb", validParams(null))
      .andReturn()
      .response.status.assert
      .isEqualTo(400)
  }

  private fun validParams(resource: String?): Map<String, String?> =
    mapOf(
      "response_type" to "code",
      "scope" to "translations.view",
      "code_challenge" to OAuth2FlowDriver.s256Challenge(OAuth2FlowDriver.randomVerifier()),
      "code_challenge_method" to "S256",
      "resource" to resource,
    )

  private fun validDocument(clientIdField: String): String =
    """
    {
      "client_id": "$clientIdField",
      "client_name": "Loopback Test Client",
      "token_endpoint_auth_method": "none",
      "grant_types": ["authorization_code", "refresh_token"],
      "redirect_uris": ["http://127.0.0.1:PLACEHOLDER/cb"]
    }
    """.trimIndent()

  /** Serves the document at `/client`, substituting the real port into the redirect once the server is bound. */
  private fun serveDocument(document: (url: String) -> String): Int {
    val created = HttpServer.create(InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0), 0)
    val port = created.address.port
    created.createContext("/client") { exchange ->
      val body =
        document("http://127.0.0.1:$port/client")
          .replace("PLACEHOLDER", port.toString())
          .toByteArray(Charsets.UTF_8)
      exchange.sendResponseHeaders(200, body.size.toLong())
      exchange.responseBody.use { it.write(body) }
    }
    created.start()
    server = created
    return port
  }
}
