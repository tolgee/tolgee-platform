package io.tolgee.mcp.tools

import io.modelcontextprotocol.client.McpSyncClient
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort

class McpTagToolsTest : AbstractMcpTest() {
  lateinit var data: McpTestData
  lateinit var client: McpSyncClient

  @BeforeEach
  fun setup() {
    data = createTestDataWithPat()
    client = createMcpClient(data.pat.token!!)
  }

  @Test
  fun `tag_key tags a key`() {
    val createResult =
      callToolAndGetJson(
        client,
        "create_key",
        mapOf("projectId" to data.projectId, "keyName" to "taggable.key"),
      )
    val keyId = createResult["id"].asLong()

    val json =
      callToolAndGetJson(
        client,
        "tag_key",
        mapOf("projectId" to data.projectId, "keyId" to keyId, "tagName" to "my-tag"),
      )
    assertThat(json["id"]).isNotNull()
    assertThat(json["name"].asText()).isEqualTo("my-tag")

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
    val createResult =
      callToolAndGetJson(
        client,
        "create_key",
        mapOf("projectId" to data.projectId, "keyName" to "tagged.key"),
      )
    val keyId = createResult["id"].asLong()
    callTool(
      client,
      "tag_key",
      mapOf("projectId" to data.projectId, "keyId" to keyId, "tagName" to "my-tag"),
    )

    val json = callToolAndGetJson(client, "list_tags", mapOf("projectId" to data.projectId))
    assertThat(json.isArray).isTrue()
    val tagNames = (0 until json.size()).map { json[it]["name"].asText() }
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
    val createResult =
      callToolAndGetJson(
        client,
        "create_key",
        mapOf("projectId" to data.projectId, "keyName" to "multi.tag.key"),
      )
    val keyId = createResult["id"].asLong()
    callTool(
      client,
      "tag_key",
      mapOf("projectId" to data.projectId, "keyId" to keyId, "tagName" to "alpha-tag"),
    )
    callTool(
      client,
      "tag_key",
      mapOf("projectId" to data.projectId, "keyId" to keyId, "tagName" to "beta-tag"),
    )

    val json =
      callToolAndGetJson(
        client,
        "list_tags",
        mapOf("projectId" to data.projectId, "search" to "alpha"),
      )
    assertThat(json.isArray).isTrue()
    val tagNames = (0 until json.size()).map { json[it]["name"].asText() }
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
