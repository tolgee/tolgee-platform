package io.tolgee.mcp

import io.tolgee.AbstractMcpTest
import io.tolgee.security.oauth2.OAuth2Constants
import io.tolgee.testing.assert
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

/**
 * The cold-start challenge: a spec-following MCP client that has never authenticated sends a bare `tools/call` and
 * needs an HTTP 401 with `WWW-Authenticate: Bearer resource_metadata="…"` — the header its OAuth machinery turns
 * into a discovery-and-login flow. An in-band JSON-RPC error is invisible to that machinery.
 */
class McpAuthChallengeTest : AbstractMcpTest() {
  @Test
  fun `a credential-less tools call is challenged with the protected-resource pointer`() {
    val response = post(TOOLS_CALL)

    response.statusCode().assert.isEqualTo(401)
    val challenge = response.headers().firstValue("WWW-Authenticate").orElse("")
    challenge.assert.startsWith("Bearer")
    challenge.assert.contains("""resource_metadata="""")
    challenge.assert.contains(OAuth2Constants.PROTECTED_RESOURCE_METADATA_PATH)
    // RFC 6750 §3.1: no error attribute when the request carried no authentication at all.
    challenge.assert.doesNotContain("error=")
  }

  @Test
  fun `initialize, the tools list and ping stay public`() {
    val client = createMcpClientWithoutAuth()

    assertThat(client.listTools().tools()).isNotEmpty
    client.ping()
  }

  @Test
  fun `a caller presenting a key in the ak query parameter is never challenged`() {
    val pakData = createTestDataWithPak()

    val response = post(TOOLS_CALL, query = "?ak=tgpak_${pakData.apiKey.encodedKey!!}")

    // The transport may still refuse the session-less call, but authentication succeeded: whatever the answer is,
    // it must not be the cold-start challenge that would break every released ?ak= setup.
    response.statusCode().assert.isNotEqualTo(401)
    response
      .headers()
      .firstValue("WWW-Authenticate")
      .isPresent.assert
      .isFalse()
  }

  @Test
  fun `an oversized body is passed through unparsed rather than challenged`() {
    val response = post("""{"pad":"${"A".repeat(McpAuthChallengeFilter.PEEK_CAP + 1024)}","method":"tools/call"}""")

    response.statusCode().assert.isNotEqualTo(401)
  }

  @Test
  fun `a batched request degrades to the transport's own refusal, not a challenge`() {
    val response = post("[$TOOLS_CALL]")

    response.statusCode().assert.isNotEqualTo(401)
  }

  private fun post(
    body: String,
    query: String = "",
  ): HttpResponse<String> {
    val request =
      HttpRequest
        .newBuilder(URI.create("http://localhost:$port${McpConstants.DEVELOPER_ENDPOINT_PATH}$query"))
        .header("Content-Type", "application/json")
        .header("Accept", "application/json, text/event-stream")
        .POST(HttpRequest.BodyPublishers.ofString(body))
        .build()
    return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString())
  }

  companion object {
    private const val TOOLS_CALL =
      """{"jsonrpc":"2.0","id":1,"method":"tools/call","params":{"name":"list_keys","arguments":{}}}"""
  }
}
