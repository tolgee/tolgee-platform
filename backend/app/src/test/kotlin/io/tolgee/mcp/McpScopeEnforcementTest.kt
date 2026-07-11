package io.tolgee.mcp

import io.modelcontextprotocol.client.McpSyncClient
import io.tolgee.AbstractMcpTest
import io.tolgee.model.enums.Scope
import org.junit.jupiter.api.Test

class McpScopeEnforcementTest : AbstractMcpTest() {
  private fun clientWithScopes(vararg scopes: Scope): McpSyncClient {
    val pakData = createTestDataWithPak(scopes.toSet())
    keyService.create(pakData.testData.projectBuilder.self, "test.key", null)
    return createMcpClientWithPak(pakData.apiKey.encodedKey!!)
  }

  @Test
  fun `KEYS_VIEW -- read key tools allowed, write key tools denied`() {
    val client = clientWithScopes(Scope.KEYS_VIEW)

    assertToolSucceeds(client, "list_keys")
    assertToolSucceeds(client, "search_keys", mapOf("query" to "test"))
    assertToolSucceeds(client, "get_key", mapOf("keyName" to "test.key"))

    assertToolFails(client, "create_keys", mapOf("keys" to listOf(mapOf("name" to "new.key"))))
    assertToolFails(client, "update_key", mapOf("keyName" to "test.key", "newName" to "renamed"))
    assertToolFails(client, "delete_keys", mapOf("keyNames" to listOf("test.key")))
  }

  @Test
  fun `KEYS_CREATE -- list_keys allowed via hierarchy, edit and delete denied`() {
    val client = clientWithScopes(Scope.KEYS_CREATE)

    // KEYS_CREATE expands to include KEYS_VIEW
    assertToolSucceeds(client, "list_keys")

    // create_keys scope gate passes but also needs TRANSLATIONS_EDIT internally
    assertToolFails(client, "update_key", mapOf("keyName" to "test.key", "newName" to "renamed"))
    assertToolFails(client, "delete_keys", mapOf("keyNames" to listOf("test.key")))
  }

  @Test
  fun `KEYS_EDIT -- update_key and tag_keys allowed, create and delete denied`() {
    val client = clientWithScopes(Scope.KEYS_EDIT)

    // KEYS_EDIT expands to include KEYS_VIEW
    assertToolSucceeds(client, "list_keys")
    assertToolSucceeds(
      client,
      "update_key",
      mapOf("keyName" to "test.key", "newName" to "renamed.key"),
    )
    assertToolSucceeds(
      client,
      "tag_keys",
      mapOf("keyNames" to listOf("renamed.key"), "tags" to listOf("my-tag")),
    )

    assertToolFails(client, "create_keys", mapOf("keys" to listOf(mapOf("name" to "new.key"))))
    assertToolFails(client, "delete_keys", mapOf("keyNames" to listOf("renamed.key")))
  }

  @Test
  fun `KEYS_DELETE -- delete_keys allowed, create and edit denied`() {
    val client = clientWithScopes(Scope.KEYS_DELETE)

    assertToolSucceeds(client, "delete_keys", mapOf("keyNames" to listOf("test.key")))

    assertToolFails(client, "create_keys", mapOf("keys" to listOf(mapOf("name" to "new.key"))))
    assertToolFails(client, "update_key", mapOf("keyName" to "x", "newName" to "y"))
  }

  @Test
  fun `LANGUAGES_EDIT -- create_language allowed, key tools denied`() {
    val client = clientWithScopes(Scope.LANGUAGES_EDIT)

    assertToolSucceeds(
      client,
      "create_language",
      mapOf("name" to "German", "tag" to "de"),
    )

    // LANGUAGES_EDIT does NOT include KEYS_VIEW
    assertToolFails(client, "list_keys")
    assertToolFails(client, "create_keys", mapOf("keys" to listOf(mapOf("name" to "new.key"))))
  }

  @Test
  fun `TRANSLATIONS_EDIT -- store_big_meta allowed, unrelated tools denied`() {
    val client = clientWithScopes(Scope.TRANSLATIONS_EDIT)

    // TRANSLATIONS_EDIT expands to include TRANSLATIONS_VIEW and KEYS_VIEW
    assertToolSucceeds(client, "list_keys")
    assertToolSucceeds(
      client,
      "store_big_meta",
      mapOf(
        "relatedKeysInOrder" to
          listOf(
            mapOf("keyName" to "test.key"),
          ),
      ),
    )

    assertToolFails(client, "create_keys", mapOf("keys" to listOf(mapOf("name" to "new.key"))))
    assertToolFails(client, "create_language", mapOf("name" to "French", "tag" to "fr"))
  }

  @Test
  fun `default-permission tools succeed with any project access`() {
    val client = clientWithScopes(Scope.KEYS_VIEW)

    assertToolSucceeds(client, "list_languages")
    assertToolSucceeds(client, "list_namespaces")
    assertToolSucceeds(client, "list_tags")
    assertToolSucceeds(client, "get_project_language_statistics")
  }

  @Test
  fun `empty scopes -- all project-scoped tools denied`() {
    val pakData = createTestDataWithPak(emptySet())
    val client = createMcpClientWithPak(pakData.apiKey.encodedKey!!)

    // Empty scopes -> ProjectNotFoundException (hides project existence from unauthorized users)
    assertToolFails(client, "list_keys", expectedError = "ProjectNotFoundException")
    assertToolFails(client, "list_languages", expectedError = "ProjectNotFoundException")
  }
}
