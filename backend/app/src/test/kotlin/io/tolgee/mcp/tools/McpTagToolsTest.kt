package io.tolgee.mcp.tools

import io.modelcontextprotocol.client.McpSyncClient
import io.tolgee.AbstractMcpTest
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort

class McpTagToolsTest : AbstractMcpTest() {
  lateinit var data: McpPakTestData
  lateinit var client: McpSyncClient

  @BeforeEach
  fun setup() {
    data = createTestDataWithPak()
    client = createMcpClientWithPak(data.apiKey.encodedKey!!)
  }

  @Test
  fun `tag_keys tags keys`() {
    callTool(
      client,
      "create_keys",
      mapOf(
        "projectId" to data.projectId,
        "keys" to listOf(mapOf("name" to "taggable.key")),
      ),
    )

    val json =
      callToolAndGetJson(
        client,
        "tag_keys",
        mapOf("projectId" to data.projectId, "keyNames" to listOf("taggable.key"), "tags" to listOf("my-tag")),
      )
    assertThat(json["tagged"].asBoolean()).isTrue()
    assertThat(json["keyCount"].asInt()).isEqualTo(1)

    val tags =
      tagService.getProjectTags(
        data.projectId,
        null,
        PageRequest.of(0, 100, Sort.by("name")),
      )
    assertThat(tags.content.map { it.name }).contains("my-tag")
  }

  @Test
  fun `list_tags returns project tags`() {
    callTool(
      client,
      "create_keys",
      mapOf(
        "projectId" to data.projectId,
        "keys" to listOf(mapOf("name" to "tagged.key")),
      ),
    )
    callTool(
      client,
      "tag_keys",
      mapOf("projectId" to data.projectId, "keyNames" to listOf("tagged.key"), "tags" to listOf("my-tag")),
    )

    val json = callToolAndGetJson(client, "list_tags", mapOf("projectId" to data.projectId))
    assertThat(json["items"].isArray).isTrue()
    val tagNames = (0 until json["items"].size()).map { json["items"][it]["name"].asText() }
    assertThat(tagNames).contains("my-tag")

    val dbTags =
      tagService.getProjectTags(
        data.projectId,
        null,
        PageRequest.of(0, 100, Sort.by("name")),
      )
    assertThat(dbTags.content.map { it.name }).contains("my-tag")
  }

  @Test
  fun `list_tags with search filter`() {
    callTool(
      client,
      "create_keys",
      mapOf(
        "projectId" to data.projectId,
        "keys" to listOf(mapOf("name" to "multi.tag.key")),
      ),
    )
    callTool(
      client,
      "tag_keys",
      mapOf(
        "projectId" to data.projectId,
        "keyNames" to listOf("multi.tag.key"),
        "tags" to listOf("alpha-tag", "beta-tag"),
      ),
    )

    val json =
      callToolAndGetJson(
        client,
        "list_tags",
        mapOf("projectId" to data.projectId, "search" to "alpha"),
      )
    assertThat(json["items"].isArray).isTrue()
    val tagNames = (0 until json["items"].size()).map { json["items"][it]["name"].asText() }
    assertThat(tagNames).contains("alpha-tag")
    assertThat(tagNames).doesNotContain("beta-tag")

    val dbTags =
      tagService.getProjectTags(
        data.projectId,
        null,
        PageRequest.of(0, 100, Sort.by("name")),
      )
    assertThat(dbTags.content.map { it.name }).contains("alpha-tag", "beta-tag")
  }
}
