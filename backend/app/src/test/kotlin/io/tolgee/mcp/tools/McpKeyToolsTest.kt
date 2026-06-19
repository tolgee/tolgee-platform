package io.tolgee.mcp.tools

import io.modelcontextprotocol.client.McpSyncClient
import io.tolgee.AbstractMcpTest
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.model.enums.Scope
import io.tolgee.repository.KeysDistanceRepository
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

  @Autowired
  lateinit var keysDistanceRepository: KeysDistanceRepository

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

  @Test
  fun `create_keys stores comments and code references on the key`() {
    callTool(
      client,
      "create_keys",
      mapOf(
        "projectId" to data.projectId,
        "keys" to
          listOf(
            mapOf(
              "name" to "meta.key",
              "comments" to listOf("Shown in the hero", "Reviewed by design"),
              "codeReferences" to
                listOf(
                  mapOf("path" to "src/Header.tsx", "line" to 42),
                  mapOf("path" to "src/Footer.tsx"),
                ),
            ),
          ),
      ),
    )

    executeInNewTransaction(transactionManager) {
      val key = keyService.find(data.projectId, "meta.key", null)
      assertThat(key).isNotNull()
      val meta = key!!.keyMeta
      assertThat(meta).isNotNull()
      assertThat(meta!!.comments.map { it.text })
        .containsExactlyInAnyOrder("Shown in the hero", "Reviewed by design")
      val refs = meta.codeReferences.associate { it.path to it.line }
      assertThat(refs).containsEntry("src/Header.tsx", 42L)
      assertThat(refs).containsEntry("src/Footer.tsx", null)
    }
  }

  @Test
  fun `create_keys stores related-key big meta for translation context`() {
    callTool(
      client,
      "create_keys",
      mapOf(
        "projectId" to data.projectId,
        "keys" to listOf(mapOf("name" to "rel.one"), mapOf("name" to "rel.two")),
        "relatedKeysInOrder" to
          listOf(
            mapOf("keyName" to "rel.one"),
            mapOf("keyName" to "rel.two"),
          ),
      ),
    )

    val key1 = keyService.find(data.projectId, "rel.one", null)!!
    waitForNotThrowing(timeout = 5000, pollTime = 100) {
      assertThat(keysDistanceRepository.getCloseKeys(key1.id)).isNotEmpty()
    }
  }

  @Test
  fun `create_keys without TRANSLATIONS_EDIT scope rejects relatedKeysInOrder and creates no keys`() {
    val restricted =
      createTestDataWithPak(
        scopes = setOf(Scope.KEYS_CREATE),
        userName = "no_translations_edit_user",
        projectName = "no_translations_edit_project",
        pakKey = "no_translations_edit_pak_key",
      )
    val restrictedClient = createMcpClientWithPak(restricted.apiKey.encodedKey!!)

    assertToolFails(
      restrictedClient,
      "create_keys",
      mapOf(
        "projectId" to restricted.projectId,
        "keys" to listOf(mapOf("name" to "rel.key")),
        "relatedKeysInOrder" to listOf(mapOf("keyName" to "rel.key")),
      ),
      expectedError = "operation_not_permitted",
    )

    assertThat(keyService.find(restricted.projectId, "rel.key", null)).isNull()
  }

  @Test
  fun `create_keys does not update comments or code references on an existing key`() {
    callTool(
      client,
      "create_keys",
      mapOf(
        "projectId" to data.projectId,
        "keys" to
          listOf(
            mapOf(
              "name" to "keep.meta",
              "comments" to listOf("first comment"),
              "codeReferences" to listOf(mapOf("path" to "src/First.tsx", "line" to 1)),
            ),
          ),
      ),
    )

    callTool(
      client,
      "create_keys",
      mapOf(
        "projectId" to data.projectId,
        "keys" to
          listOf(
            mapOf(
              "name" to "keep.meta",
              "comments" to listOf("second comment"),
              "codeReferences" to listOf(mapOf("path" to "src/Second.tsx", "line" to 2)),
            ),
          ),
      ),
    )

    executeInNewTransaction(transactionManager) {
      val key = keyService.find(data.projectId, "keep.meta", null)!!
      assertThat(key.keyMeta!!.comments.map { it.text }).containsExactly("first comment")
      assertThat(key.keyMeta!!.codeReferences.map { it.path }).containsExactly("src/First.tsx")
    }
  }

  @Test
  fun `create_keys stores big meta using the default namespace for related keys`() {
    callTool(
      client,
      "create_keys",
      mapOf(
        "projectId" to data.projectId,
        "namespace" to "ui",
        "keys" to listOf(mapOf("name" to "ns.rel.one"), mapOf("name" to "ns.rel.two")),
        "relatedKeysInOrder" to
          listOf(
            mapOf("keyName" to "ns.rel.one"),
            mapOf("keyName" to "ns.rel.two"),
          ),
      ),
    )

    val key1 = keyService.find(data.projectId, "ns.rel.one", "ui")!!
    waitForNotThrowing(timeout = 5000, pollTime = 100) {
      assertThat(keysDistanceRepository.getCloseKeys(key1.id)).isNotEmpty()
    }
  }

  @Test
  fun `create_keys rejects an over-long code reference path and creates no keys`() {
    val longPath = "src/" + "x".repeat(400)

    assertToolFails(
      client,
      "create_keys",
      mapOf(
        "projectId" to data.projectId,
        "keys" to listOf(mapOf("name" to "longpath.key", "codeReferences" to listOf(mapOf("path" to longPath)))),
      ),
      expectedError = "key_code_reference_path_too_long",
    )

    assertThat(keyService.find(data.projectId, "longpath.key", null)).isNull()
  }

  @Test
  fun `create_keys rejects an over-long comment and creates no keys`() {
    assertToolFails(
      client,
      "create_keys",
      mapOf(
        "projectId" to data.projectId,
        "keys" to listOf(mapOf("name" to "longcomment.key", "comments" to listOf("x".repeat(2001)))),
      ),
      expectedError = "key_comment_too_long",
    )

    assertThat(keyService.find(data.projectId, "longcomment.key", null)).isNull()
  }

  @Test
  fun `create_keys skips blank comments and code references`() {
    callTool(
      client,
      "create_keys",
      mapOf(
        "projectId" to data.projectId,
        "keys" to
          listOf(
            mapOf(
              "name" to "blank.meta.key",
              "comments" to listOf("  "),
              "codeReferences" to listOf(mapOf("path" to "")),
            ),
          ),
      ),
    )

    executeInNewTransaction(transactionManager) {
      val key = keyService.find(data.projectId, "blank.meta.key", null)!!
      assertThat(key.keyMeta?.comments ?: emptyList()).isEmpty()
      assertThat(key.keyMeta?.codeReferences ?: emptyList()).isEmpty()
    }
  }

  @Test
  fun `create_keys rolls back the whole batch when one key is invalid`() {
    assertToolFails(
      client,
      "create_keys",
      mapOf(
        "projectId" to data.projectId,
        "keys" to
          listOf(
            mapOf("name" to "good.key"),
            mapOf("name" to "bad.key", "comments" to listOf("x".repeat(2001))),
          ),
      ),
      expectedError = "key_comment_too_long",
    )

    assertThat(keyService.find(data.projectId, "good.key", null)).isNull()
    assertThat(keyService.find(data.projectId, "bad.key", null)).isNull()
  }

  @Test
  fun `create_keys stores big meta for related keys with an explicit namespace`() {
    callTool(
      client,
      "create_keys",
      mapOf(
        "projectId" to data.projectId,
        "keys" to
          listOf(
            mapOf("name" to "comp.one", "namespace" to "comp"),
            mapOf("name" to "comp.two", "namespace" to "comp"),
          ),
        "relatedKeysInOrder" to
          listOf(
            mapOf("keyName" to "comp.one", "namespace" to "comp"),
            mapOf("keyName" to "comp.two", "namespace" to "comp"),
          ),
      ),
    )

    val key1 = keyService.find(data.projectId, "comp.one", "comp")!!
    waitForNotThrowing(timeout = 5000, pollTime = 100) {
      assertThat(keysDistanceRepository.getCloseKeys(key1.id)).isNotEmpty()
    }
  }
}
