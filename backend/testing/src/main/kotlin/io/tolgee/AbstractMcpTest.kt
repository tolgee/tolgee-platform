package io.tolgee

import com.fasterxml.jackson.databind.JsonNode
import io.modelcontextprotocol.client.McpClient
import io.modelcontextprotocol.client.McpSyncClient
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport
import io.modelcontextprotocol.spec.McpSchema
import io.tolgee.development.testDataBuilder.data.BaseTestData
import io.tolgee.model.Pat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.TestInstance
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import java.time.Duration

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class AbstractMcpTest : AbstractSpringTest() {
  @LocalServerPort
  protected val port: Int = 0

  private val clients = mutableListOf<McpSyncClient>()

  fun createMcpClient(patToken: String): McpSyncClient {
    val transport =
      HttpClientStreamableHttpTransport
        .builder("http://localhost:$port")
        .endpoint("/mcp/developer")
        .customizeRequest { builder ->
          builder.header("X-API-Key", "tgpat_$patToken")
        }.build()

    val client =
      McpClient
        .sync(transport)
        .clientInfo(McpSchema.Implementation("test-client", "1.0"))
        .requestTimeout(Duration.ofSeconds(10))
        .build()

    client.initialize()
    clients.add(client)
    return client
  }

  @AfterEach
  fun closeMcpClients() {
    clients.forEach {
      try {
        it.close()
      } catch (_: Exception) {
      }
    }
    clients.clear()
  }

  fun createTestDataWithPat(): McpTestData {
    var pat: Pat? = null
    val base = BaseTestData()
    base.userAccountBuilder.build {
      addPat {
        description = "MCP test PAT"
        pat = this
      }
    }
    base.projectBuilder.addBranch {
      name = "main"
      isDefault = true
    }
    testDataService.saveTestData(base.root)
    return McpTestData(
      testData = base,
      pat = pat!!,
      projectId = base.project.id,
      organizationId = base.projectBuilder.self.organizationOwner.id,
    )
  }

  fun callTool(
    client: McpSyncClient,
    name: String,
    arguments: Map<String, Any?> = emptyMap(),
  ): McpSchema.CallToolResult {
    return client.callTool(McpSchema.CallToolRequest(name, arguments))
  }

  fun callToolAndGetJson(
    client: McpSyncClient,
    name: String,
    arguments: Map<String, Any?> = emptyMap(),
  ): JsonNode {
    val result = callTool(client, name, arguments)
    val textContent = result.content().first() as McpSchema.TextContent
    return objectMapper.readTree(textContent.text())
  }

  data class McpTestData(
    val testData: BaseTestData,
    val pat: Pat,
    val projectId: Long,
    val organizationId: Long,
  )
}
