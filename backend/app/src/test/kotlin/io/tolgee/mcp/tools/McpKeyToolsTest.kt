package io.tolgee.mcp.tools

import io.modelcontextprotocol.client.McpSyncClient
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class McpKeyToolsTest : AbstractMcpTest() {
  lateinit var data: McpTestData
  lateinit var client: McpSyncClient

  @BeforeEach
  fun setup() {
    data = createTestDataWithPat()
    client = createMcpClient(data.pat.token!!)
  }

  @Test
  fun `list_keys returns empty for project with no keys`() {
    val json = callToolAndGetJson(client, "list_keys", mapOf("projectId" to data.projectId))
    assertThat(json["keys"].isArray).isTrue()
    assertThat(json["keys"].size()).isEqualTo(0)
    assertThat(json["totalKeys"].asLong()).isEqualTo(0)
    assertThat(json["hasMore"].asBoolean()).isFalse()
  }

  @Test
  fun `list_keys returns project keys`() {
    listOf("first.key", "second.key", "third.key").forEach { name ->
      keyService.create(data.testData.projectBuilder.self, name, null)
    }

    val json = callToolAndGetJson(client, "list_keys", mapOf("projectId" to data.projectId))
    assertThat(json["keys"].isArray).isTrue()
    assertThat(json["totalKeys"].asLong()).isEqualTo(3)
    assertThat(json["hasMore"].asBoolean()).isFalse()
    val keyNames = (0 until json["keys"].size()).map { json["keys"][it]["keyName"].asText() }
    assertThat(keyNames).containsExactlyInAnyOrder("first.key", "second.key", "third.key")
  }

  @Test
  fun `create_key creates a key`() {
    val json =
      callToolAndGetJson(
        client,
        "create_key",
        mapOf(
          "projectId" to data.projectId,
          "keyName" to "home.welcome",
        ),
      )
    assertThat(json["id"]).isNotNull()
    assertThat(json["name"].asText()).isEqualTo("home.welcome")

    val key = keyService.get(json["id"].asLong())
    assertThat(key.name).isEqualTo("home.welcome")
    assertThat(key.project.id).isEqualTo(data.projectId)
  }

  @Test
  fun `search_keys finds created key`() {
    callTool(
      client,
      "create_key",
      mapOf("projectId" to data.projectId, "keyName" to "search.target"),
    )
    val json =
      callToolAndGetJson(
        client,
        "search_keys",
        mapOf("projectId" to data.projectId, "query" to "search.target"),
      )
    assertThat(json.isArray).isTrue()
    val keyNames = (0 until json.size()).map { json[it]["keyName"].asText() }
    assertThat(keyNames).contains("search.target")
  }

  @Test
  fun `get_key returns key details`() {
    val createResult =
      callToolAndGetJson(
        client,
        "create_key",
        mapOf("projectId" to data.projectId, "keyName" to "detail.key"),
      )
    val keyId = createResult["id"].asLong()

    val json =
      callToolAndGetJson(
        client,
        "get_key",
        mapOf("projectId" to data.projectId, "keyId" to keyId),
      )
    assertThat(json["keyId"].asLong()).isEqualTo(keyId)
    assertThat(json["keyName"].asText()).isEqualTo("detail.key")
  }

  @Test
  fun `update_key changes name`() {
    val createResult =
      callToolAndGetJson(
        client,
        "create_key",
        mapOf("projectId" to data.projectId, "keyName" to "old.name"),
      )
    val keyId = createResult["id"].asLong()

    val json =
      callToolAndGetJson(
        client,
        "update_key",
        mapOf("projectId" to data.projectId, "keyId" to keyId, "name" to "new.name"),
      )
    assertThat(json["name"].asText()).isEqualTo("new.name")

    val key = keyService.get(keyId)
    assertThat(key.name).isEqualTo("new.name")
  }

  @Test
  fun `batch_create_keys creates multiple`() {
    val json =
      callToolAndGetJson(
        client,
        "batch_create_keys",
        mapOf(
          "projectId" to data.projectId,
          "keys" to
            listOf(
              mapOf("name" to "batch.one", "translations" to mapOf("en" to "One")),
              mapOf("name" to "batch.two", "translations" to mapOf("en" to "Two")),
            ),
        ),
      )
    assertThat(json).isNotNull()

    val keys = keyService.getAll(data.projectId)
    val keyNames = keys.map { it.name }
    assertThat(keyNames).contains("batch.one", "batch.two")
  }
}
