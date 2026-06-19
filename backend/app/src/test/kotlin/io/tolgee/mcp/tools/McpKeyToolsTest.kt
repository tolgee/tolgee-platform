package io.tolgee.mcp.tools

import io.modelcontextprotocol.client.McpSyncClient
import io.tolgee.AbstractMcpTest
import io.tolgee.testing.assertions.Assertions.assertThat
import io.tolgee.util.executeInNewTransaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.PlatformTransactionManager

class McpKeyToolsTest : AbstractMcpTest() {
  lateinit var data: McpPakTestData
  lateinit var client: McpSyncClient

  @Autowired
  lateinit var transactionManager: PlatformTransactionManager

  @BeforeEach
  fun setup() {
    data = createTestDataWithPak()
    client = createMcpClientWithPak(data.apiKey.encodedKey!!)
  }

  @Test
  fun `list_keys auto-resolves projectId from PAK`() {
    keyService.create(data.testData.projectBuilder.self, "auto.key", null)

    val json = callToolAndGetJson(client, "list_keys")
    assertThat(json["items"].isArray).isTrue()
    assertThat(json["totalItems"].asLong()).isEqualTo(1)
  }

  @Test
  fun `list_keys returns empty for project with no keys`() {
    val json = callToolAndGetJson(client, "list_keys", mapOf("projectId" to data.projectId))
    assertThat(json["items"].isArray).isTrue()
    assertThat(json["items"].size()).isEqualTo(0)
    assertThat(json["totalItems"].asLong()).isEqualTo(0)
  }

  @Test
  fun `list_keys returns project keys`() {
    listOf("first.key", "second.key", "third.key").forEach { name ->
      keyService.create(data.testData.projectBuilder.self, name, null)
    }

    val json = callToolAndGetJson(client, "list_keys", mapOf("projectId" to data.projectId))
    assertThat(json["items"].isArray).isTrue()
    assertThat(json["totalItems"].asLong()).isEqualTo(3)
    assertThat(json["page"].asInt()).isEqualTo(0)
    assertThat(json["totalPages"].asInt()).isEqualTo(1)
    val keyNames = (0 until json["items"].size()).map { json["items"][it]["keyName"].asText() }
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
    assertThat(json["items"].isArray).isTrue()
    val keyNames = (0 until json["items"].size()).map { json["items"][it]["keyName"].asText() }
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

  @Test
  fun `create_keys stores structured custom metadata on the key`() {
    val custom =
      mapOf(
        "reactComponent" to "SignUpButton",
        "primary" to true,
        "order" to 3,
        "aliases" to listOf("signup", "register"),
        "props" to mapOf("variant" to "filled"),
      )

    callTool(
      client,
      "create_keys",
      mapOf(
        "projectId" to data.projectId,
        "keys" to listOf(mapOf("name" to "custom.key", "custom" to custom)),
      ),
    )

    executeInNewTransaction(transactionManager) {
      val key = keyService.find(data.projectId, "custom.key", null)
      assertThat(key).isNotNull()
      // Full structured payload (bool, number, array, nested object) must survive the JSONB round-trip.
      assertThat(key!!.keyMeta?.custom).isEqualTo(custom)
    }
  }

  @Test
  fun `create_keys rejects oversized custom values`() {
    val oversized = "x".repeat(6000)

    assertToolFails(
      client,
      "create_keys",
      mapOf(
        "projectId" to data.projectId,
        "keys" to listOf(mapOf("name" to "toobig.key", "custom" to mapOf("blob" to oversized))),
      ),
      expectedError = "custom_values_json_too_long",
    )

    assertThat(keyService.find(data.projectId, "toobig.key", null)).isNull()
  }

  @Test
  fun `create_keys does not overwrite custom on an already-existing key`() {
    callTool(
      client,
      "create_keys",
      mapOf(
        "projectId" to data.projectId,
        "keys" to listOf(mapOf("name" to "keep.custom", "custom" to mapOf("owner" to "first"))),
      ),
    )

    callTool(
      client,
      "create_keys",
      mapOf(
        "projectId" to data.projectId,
        "keys" to listOf(mapOf("name" to "keep.custom", "custom" to mapOf("owner" to "second"))),
      ),
    )

    executeInNewTransaction(transactionManager) {
      val key = keyService.find(data.projectId, "keep.custom", null)
      assertThat(key).isNotNull()
      assertThat(key!!.keyMeta?.custom).isEqualTo(mapOf("owner" to "first"))
    }
  }
}
