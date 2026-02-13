package io.tolgee.mcp.tools

import io.modelcontextprotocol.client.McpSyncClient
import io.tolgee.AbstractMcpTest
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class McpLanguageToolsTest : AbstractMcpTest() {
  lateinit var data: McpPakTestData
  lateinit var client: McpSyncClient

  @BeforeEach
  fun setup() {
    data = createTestDataWithPak()
    client = createMcpClientWithPak(data.apiKey.encodedKey!!)
  }

  @Test
  fun `list_languages auto-resolves projectId from PAK`() {
    val json = callToolAndGetJson(client, "list_languages")
    assertThat(json["items"].isArray).isTrue()
    val tags = (0 until json["items"].size()).map { json["items"][it]["tag"].asText() }
    assertThat(tags).contains("en")
  }

  @Test
  fun `list_languages returns project languages`() {
    val json = callToolAndGetJson(client, "list_languages", mapOf("projectId" to data.projectId))
    assertThat(json["items"].isArray).isTrue()
    val tags = (0 until json["items"].size()).map { json["items"][it]["tag"].asText() }
    assertThat(tags).contains("en")
  }

  @Test
  fun `create_language adds a new language`() {
    val json =
      callToolAndGetJson(
        client,
        "create_language",
        mapOf("projectId" to data.projectId, "name" to "German", "tag" to "de"),
      )
    assertThat(json["id"]).isNotNull()
    assertThat(json["tag"].asText()).isEqualTo("de")

    val language = languageService.getEntity(json["id"].asLong())
    assertThat(language.tag).isEqualTo("de")
    assertThat(language.name).isEqualTo("German")
    assertThat(language.project.id).isEqualTo(data.projectId)
  }

  @Test
  fun `list_namespaces returns namespaces`() {
    // Create a key with a namespace
    callTool(
      client,
      "create_keys",
      mapOf(
        "projectId" to data.projectId,
        "keys" to listOf(mapOf("name" to "ns.key", "namespace" to "my-namespace")),
      ),
    )

    val json = callToolAndGetJson(client, "list_namespaces", mapOf("projectId" to data.projectId))
    assertThat(json.isArray).isTrue()
    val nsNames = (0 until json.size()).mapNotNull { json[it]["name"]?.asText() }
    assertThat(nsNames).contains("my-namespace")
  }
}
