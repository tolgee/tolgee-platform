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
  fun `create_keys creates keys`() {
    val json =
      callToolAndGetJson(
        client,
        "create_keys",
        mapOf(
          "projectId" to data.projectId,
          "keys" to
            listOf(
              mapOf("name" to "home.welcome"),
              mapOf("name" to "home.subtitle", "translations" to mapOf("en" to "Welcome")),
            ),
        ),
      )
    assertThat(json["created"].asBoolean()).isTrue()
    assertThat(json["keyCount"].asInt()).isEqualTo(2)

    val keys = keyService.getAll(data.projectId)
    val keyNames = keys.map { it.name }
    assertThat(keyNames).contains("home.welcome", "home.subtitle")
  }

  @Test
  fun `search_keys finds created key`() {
    callTool(
      client,
      "create_keys",
      mapOf(
        "projectId" to data.projectId,
        "keys" to listOf(mapOf("name" to "search.target")),
      ),
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
  fun `get_key returns key details by name`() {
    callTool(
      client,
      "create_keys",
      mapOf(
        "projectId" to data.projectId,
        "keys" to listOf(mapOf("name" to "detail.key")),
      ),
    )

    val json =
      callToolAndGetJson(
        client,
        "get_key",
        mapOf("projectId" to data.projectId, "keyName" to "detail.key"),
      )
    assertThat(json["keyId"]).isNotNull()
    assertThat(json["keyName"].asText()).isEqualTo("detail.key")
  }

  @Test
  fun `update_key changes name by key name lookup`() {
    callTool(
      client,
      "create_keys",
      mapOf(
        "projectId" to data.projectId,
        "keys" to listOf(mapOf("name" to "old.name")),
      ),
    )

    val json =
      callToolAndGetJson(
        client,
        "update_key",
        mapOf(
          "projectId" to data.projectId,
          "keyName" to "old.name",
          "newName" to "new.name",
        ),
      )
    assertThat(json["name"].asText()).isEqualTo("new.name")

    val key = keyService.find(data.projectId, "new.name", null)
    assertThat(key).isNotNull()
    assertThat(key!!.name).isEqualTo("new.name")
  }

  @Test
  fun `delete_keys deletes keys by name`() {
    callTool(
      client,
      "create_keys",
      mapOf(
        "projectId" to data.projectId,
        "keys" to
          listOf(
            mapOf("name" to "delete.one"),
            mapOf("name" to "delete.two"),
            mapOf("name" to "keep.me"),
          ),
      ),
    )

    val json =
      callToolAndGetJson(
        client,
        "delete_keys",
        mapOf(
          "projectId" to data.projectId,
          "keyNames" to listOf("delete.one", "delete.two"),
        ),
      )
    assertThat(json["deleted"].asBoolean()).isTrue()
    assertThat(json["keyCount"].asInt()).isEqualTo(2)

    assertThat(keyService.find(data.projectId, "delete.one", null)).isNull()
    assertThat(keyService.find(data.projectId, "delete.two", null)).isNull()
    assertThat(keyService.find(data.projectId, "keep.me", null)).isNotNull()
  }

  @Test
  fun `delete_keys returns error for non-existent key`() {
    val result =
      callTool(
        client,
        "delete_keys",
        mapOf(
          "projectId" to data.projectId,
          "keyNames" to listOf("does.not.exist"),
        ),
      )
    assertThat(result.isError).isTrue()
  }
}
