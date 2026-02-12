package io.tolgee.mcp

import io.modelcontextprotocol.client.McpClient
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport
import io.modelcontextprotocol.spec.McpSchema
import io.tolgee.AbstractMcpTest
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
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
  fun `listTools returns all 22 tools`() {
    val client = createMcpClient(data.pat.token!!)
    val tools = client.listTools()
    val toolNames = tools.tools().map { it.name() }.toSet()
    assertThat(toolNames).containsExactlyInAnyOrder(
      "list_projects",
      "create_project",
      "get_project_language_statistics",
      "list_keys",
      "search_keys",
      "create_keys",
      "get_key",
      "update_key",
      "delete_keys",
      "list_languages",
      "create_language",
      "get_translations",
      "set_translation",
      "list_tags",
      "tag_keys",
      "list_namespaces",
      "get_batch_job_status",
      "machine_translate",
      "store_big_meta",
      "list_branches",
      "create_branch",
      "delete_branch",
    )
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
      client.initialize()
      val result = callTool(client, "list_projects")
      // If we get here, the call should have returned an error
      assertThat(result.isError).isTrue()
    } catch (e: Exception) {
      // Expected — auth failure
      assertThat(e).isNotNull()
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
      client.initialize()
      val result = callTool(client, "list_projects")
      assertThat(result.isError).isTrue()
    } catch (e: Exception) {
      assertThat(e).isNotNull()
    } finally {
      try {
        client.close()
      } catch (_: Exception) {
      }
    }
  }
}
