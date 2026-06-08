package io.tolgee.mcp

import io.modelcontextprotocol.client.McpClient
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport
import io.modelcontextprotocol.spec.McpSchema
import io.tolgee.API_KEY_HEADER_NAME
import io.tolgee.AbstractMcpTest
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.Date

class McpAuthenticationTest : AbstractMcpTest() {
  lateinit var data: McpPatTestData

  @BeforeEach
  fun setup() {
    data = createTestDataWithPat()
  }

  @Test
  fun `initialize succeeds with valid PAT`() {
    val client = createMcpClientWithPat(data.pat.token!!)
    val tools = client.listTools()
    assertThat(tools.tools()).isNotEmpty
  }

  @Test
  fun `initialize succeeds with valid PAK`() {
    val pakData = createTestDataWithPak()
    val client = createMcpClientWithPak(pakData.apiKey.encodedKey!!)
    val tools = client.listTools()
    assertThat(tools.tools()).isNotEmpty
  }

  @Test
  fun `callTool succeeds with PAK for project-scoped tool`() {
    val pakData = createTestDataWithPak()
    val client = createMcpClientWithPak(pakData.apiKey.encodedKey!!)
    val result = callToolAndGetJson(client, "list_keys", mapOf("projectId" to pakData.projectId))
    assertThat(result["items"]).isNotNull
  }

  @Test
  fun `callTool fails without auth`() {
    val transport =
      HttpClientStreamableHttpTransport
        .builder("http://localhost:$port")
        .endpoint("/mcp/developer")
        .customizeRequest { builder ->
          builder.header("X-API-Key", "tgpat_invalid_token")
        }.build()

    val client =
      McpClient
        .sync(transport)
        .clientInfo(McpSchema.Implementation("test-client", "1.0"))
        .requestTimeout(Duration.ofSeconds(10))
        .build()

    try {
      assertThrows<Exception> {
        client.initialize()
        callTool(client, "list_projects")
      }
    } finally {
      try {
        client.close()
      } catch (_: Exception) {
      }
    }
  }

  @Test
  fun `mcp endpoint returns 401 with WWW-Authenticate when called without credentials`() {
    val response = postMcpInitialize(apiKey = null)

    assertThat(response.statusCode()).isEqualTo(401)
    val wwwAuth = response.headers().firstValue("WWW-Authenticate").orElse(null)
    assertThat(wwwAuth).isNotNull()
    assertThat(wwwAuth).startsWith("Bearer ")
  }

  @Test
  fun `mcp endpoint returns 401 with WWW-Authenticate when called with invalid PAT`() {
    val response = postMcpInitialize(apiKey = "tgpat_invalid_token")

    assertThat(response.statusCode()).isEqualTo(401)
    val wwwAuth = response.headers().firstValue("WWW-Authenticate").orElse(null)
    assertThat(wwwAuth).isNotNull()
    assertThat(wwwAuth).startsWith("Bearer ")
  }

  private fun postMcpInitialize(apiKey: String?): HttpResponse<String> {
    val client = HttpClient.newHttpClient()
    val body =
      """{"jsonrpc":"2.0","id":1,"method":"initialize","params":{""" +
        """"protocolVersion":"2024-11-05","capabilities":{},""" +
        """"clientInfo":{"name":"raw-test-client","version":"1.0"}}}"""
    val builder =
      HttpRequest
        .newBuilder()
        .uri(URI.create("http://localhost:$port/mcp/developer"))
        .header("Content-Type", "application/json")
        .header("Accept", "application/json, text/event-stream")
        .POST(HttpRequest.BodyPublishers.ofString(body))
    if (apiKey != null) {
      builder.header(API_KEY_HEADER_NAME, apiKey)
    }
    return client.send(builder.build(), HttpResponse.BodyHandlers.ofString())
  }

  @Test
  fun `callTool fails with expired PAT`() {
    var expiredPat: io.tolgee.model.Pat? = null
    testDataService.saveTestData {
      addUserAccount {
        username = "expired_pat_user"
      }.build {
        addPat {
          description = "Expired PAT"
          expiresAt = Date(System.currentTimeMillis() - 100000)
          expiredPat = this
        }
      }
    }

    val transport =
      HttpClientStreamableHttpTransport
        .builder("http://localhost:$port")
        .endpoint("/mcp/developer")
        .customizeRequest { builder ->
          builder.header("X-API-Key", "tgpat_${expiredPat!!.token}")
        }.build()

    val client =
      McpClient
        .sync(transport)
        .clientInfo(McpSchema.Implementation("test-client", "1.0"))
        .requestTimeout(Duration.ofSeconds(10))
        .build()

    try {
      assertThrows<Exception> {
        client.initialize()
        callTool(client, "list_projects")
      }
    } finally {
      try {
        client.close()
      } catch (_: Exception) {
      }
    }
  }
}
