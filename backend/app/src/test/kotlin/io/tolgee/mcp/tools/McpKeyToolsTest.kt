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
