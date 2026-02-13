package io.tolgee.mcp

import io.modelcontextprotocol.client.McpClient
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport
import io.modelcontextprotocol.spec.McpSchema
import io.tolgee.AbstractMcpTest
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Duration
import java.util.Date

class McpAuthenticationTest : AbstractMcpTest() {
  lateinit var data: McpTestData

  @BeforeEach
  fun setup() {
    data = createTestDataWithPat()
  }

  @Test
  fun `initialize succeeds with valid PAT`() {
    val client = createMcpClient(data.pat.token!!)
    val tools = client.listTools()
    assertThat(tools.tools()).isNotEmpty
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
